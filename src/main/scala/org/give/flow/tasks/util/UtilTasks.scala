/*
# Flow util Tasks
*/
package org.give.flow.tasks.util

import org.give.flow.tasks._

import java.util.Date

/*
## Dependencies
### scala xml io concurrent url

*/
import scala.xml._
import scala.io._
import scala.concurrent._
import java.nio.charset._
import java.net.URL
import java.io._

/* 
## Dependencies
### exceutioncontext global for Future 
*/

import ExecutionContext.Implicits.global
/* 
## Given a URL retrieve it and generate string output 
*/
case class DownloadURLTask ( 
	override val specName: String , 
	val url: String, 
	val saveToFile: Boolean = false,
	val saveToFileName: String = "",
	var page:Int = 1  /*  use page as a general mechanism to access RESTFUL urls 
							that support paging. todo generalize parameters as generalized dictionary */
	,override val nextTask: TaskBase = null,
	val useCache:Boolean = true
) extends Task[String, String]  
{
	 var _tries:Int = 1

	input = url
//	specName = name
	override def loopIncrement(): Int= {
		this.synchronized { 
			page = page + 1
			println("loopIncrement " + page)
			return page
		}
	}
	override def loopIndex(): Int = {
		return page
	}
	// using Tomasz Nurkiewicz  async approach
	def downloadPage(url: String) = Future {
	    scala.io.Source.fromURL(new URL( url ) ).mkString
	}
	override def act( replyTo: akka.actor.ActorRef) = { 
		//System.out.println( "Task DownloadURLTask" + input ); 


		var fetchURL = url 
		if ( page > 1  ) {
			if( fetchURL.indexOf("?") > 0 ) {
				fetchURL = fetchURL + "&"
			}
			else {
				fetchURL = fetchURL + "?"
			}
			fetchURL = fetchURL + "page=" + page.toString()   
		}
		println("URL: "  + fetchURL)

		var fileName = saveToFileName
		if( fileName == null || fileName.length == 0 ) {
			fileName = """\/|\?|\=|\:|\.|\&""".r.replaceAllIn (fetchURL ,  "_")
			fileName = fileName.replace("http___", "")
			fileName = fileName.replace("https___", "")
			//todo use ContentType header
			if(url.indexOf(".xml") > 0 ) {
				fileName = fileName + ".xml"
			}
			else if(url.indexOf(".json") > 0) {
				fileName = fileName + ".json"
			}
			else {
				fileName = fileName + ".txt"
			}
		}
		var webCacheFile:File = new File("./web/" + fileName )
		if( useCache && webCacheFile.exists() ) {
			//println("read from cache for URL " + fetchURL )
			val webURL:String = fetchURL
			fetchURL = "file://" + System.getProperty("user.dir") + "/web/" + fileName ;
			//println("using cache file: " + fetchURL + " for " + webURL)
		}
	 	val downloadStringFuture: Future[String] = downloadPage (  fetchURL  )

	 	downloadStringFuture  onFailure    {
	 				case t => {  	
	 					println("onFailure downloadStringFuture ")
	 					// do not send error
	 					// try three times 
	 					_tries = _tries + 1
	 					if(_tries  >= 3 ) {
	 						println("3 Tries failed for " + fetchURL )
	 						replyTo ! processed(  false, t.getMessage , "error")
	 					}
	 					else { 
	 						println("Error downloading " + fetchURL +", retry ....")
	 						act( replyTo )
	 					}
	 				}
	 			} 
	 	downloadStringFuture  onSuccess   {
	 				case result => { 
			 			//println("download done " + fetchURL )
			 			if( saveToFile && fetchURL.indexOf("file://") != 0 ) {
			 				
			 				printToFile( new File("./web/" + fileName))(p => {p.println(result) })

			 			}
	 					replyTo ! processed(  true, "downloaded" , result)
	 				}
	 			} 

	}
	/* http://stackoverflow.com/a/4608061 */
	def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
  		val writer = new java.io.PrintWriter(f)
  		try { 
  			op( writer ) 
  		} 
  		finally { 
  			writer.close() 
  		}
	}
	/* todo
	def readString( path:String ):String = {
		var encoding:Charset 
		var encoded:Array[Byte]  = Files.readAllBytes(Paths.get(path))
  		return encoding.decode(ByteBuffer.wrap(encoded)).toString()
 	
	}*/
}


/*
	Given a string XML parse and return XML Elem as output 
*/
case class XmlParseTask ( override val specName: String 
	, override val nextTask: TaskBase = null
) extends Task[String, Elem ]  
{
	def parseXML(xmlString: String):Elem =   {
	    XML.loadString(xmlString)
	}


	override def act( replyTo: akka.actor.ActorRef) = { 
	 	val userXml: Elem =  parseXML( input )
		replyTo ! processed(  true, "xml loaded" , userXml )
	}
}


/* given an XML document find an element */

case class ParseNameTask( override val specName: String , val elementName : String, 
	val anyDescendants: Boolean = false,
	override val nextTask: TaskBase = null
) extends Task[Elem, String]  {

	 def parseName(xElem: Elem) =   {
	 	if(anyDescendants)
	 		xElem  \\ elementName text 
	 	else
	 		xElem  \ elementName text 
	 }
	override def act( replyTo: akka.actor.ActorRef) = { 
	 	val  userName = parseName( input )
	 	// TODO: handle what should happen if it is not found
	 	if( userName == null || userName.compareTo("") == 0 ) {
	 		
	 		replyTo ! processed(  false, "element " + elementName + " not found " , null )
	 	}
	 	else {
	 		
			replyTo ! processed(  true, "element " + elementName + " found " , userName )
		}
	}
}


/*
	given a task loop back to it, stop if loopUntil returns false
*/
case  class LoopbackTask ( override val specName:String = "LoopbackTask", 
	var backToTask : () => TaskBase , 
	var loopUntil: ( Task[AnyRef,AnyRef] ) => Boolean ,
	var endTask : TaskBase
	/* loopback can't have a next task for now ,
	override val nextTask: TaskBase = null */ 
) extends Task[ AnyRef , AnyRef]  
{
	
	override def act(taskMover: akka.actor.ActorRef ) {

		val task = backToTask()
		val iteration:Int = task.loopIncrement()

		if(   loopUntil(this)  ) {
			output = "loop"
			taskMover ! task
			//taskMover ! processed(  true, "loop # " +  task.loopIndex , "end" )
		}
		else {
			output = "end"
			//taskMover ! processed(  true, "loop ended", "end" )
			taskMover ! endTask
		}
	}

}

/* print input to console */
case  class PrintInput ( override val specName:String = "PrintInput")  extends Task[ AnyRef , AnyRef]  
{
	
	override def act(taskMover: akka.actor.ActorRef ) {
		println (input.toString)
		output = "done"
	}

}


/*
	given a task output pass it as input to multiple tasks at once and execute. 
*/
case  class CompositeTasks ( override val specName:String = "CompositeTasks", 
	var tasks : Seq[TaskBase] , var completedTasks: List[TaskBase] = List()
) extends Task[ AnyRef , AnyRef]  
{
	override def observedTaskDone(replyTo: akka.actor.ActorRef, who:TaskBase ) : Boolean= {
		//println("CompositeTasks observedTaskDone recieved " + who.specName )
		var ret = super.observedTaskDone(replyTo, who)
		if(ret) { 
			replyTo ! this.processed( true, "composite all done", "composite all done") 
			//println("All composite tasks done")
		}
		return ret
	}
	override def act(taskMover: akka.actor.ActorRef ) {
		tasks map ( (x)=> { 
			
			waitFor(x)
		 	x.setInput(this.input)
		 	taskMover ! x 
		 	})
		output = "done"
	}

}

