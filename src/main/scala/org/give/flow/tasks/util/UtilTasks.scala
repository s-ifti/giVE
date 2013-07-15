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
	var page:Int = 1  /*  use page as a general mechanism to access RESTFUL urls 
							that support paging. todo generalize parameters as generalized dictionary */
	,override val nextTask: TaskBase = null
) extends Task[String, String]  
{
	 
	input = url
//	specName = name
	override def loopIncrement(): Int= {
		page = page + 1
		return page
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
				fetchURL += "&"
			}
			else {
				fetchURL += "?"
			}
			fetchURL += ("page=" + page.toString() )  
		}
		println("URL: "  + fetchURL)

	 	val downloadStringFuture: Future[String] = downloadPage (  fetchURL  )

	 	downloadStringFuture  onFailure    {
	 				case t => {  	
	 					println("onFailure downloadStringFuture ")
	 					replyTo ! processed(  false, t.getMessage , "error")
	 				}
	 			} 
	 	downloadStringFuture  onSuccess   {
	 				case result => { 
			 			//println("onSuccess  downloadStringFuture " + fetchURL )
	 					replyTo ! processed(  true, "downloaded" , result)
	 				}
	 			} 

	}
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
	var loopUntil: ( Task[AnyRef,AnyRef] ) => Boolean
	/* loopback can't have a next task for now ,
	override val nextTask: TaskBase = null */ 
) extends Task[ AnyRef , AnyRef]  
{
	
	override def act(taskMover: akka.actor.ActorRef ) {

		val task = backToTask()
		val iteration = task.loopIncrement()

		if(   loopUntil(this)  ) {
			println( "iteration # " + iteration)
			output = "loop"
			taskMover ! task
			taskMover ! processed(  true, "loop # " +  task.loopIndex , "end" )
		}
		else {
			output = "end"
			println ("end loop")
			taskMover ! processed(  true, "loop ended", "end" )
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
	var tasks : Seq[TaskBase] 
) extends Task[ AnyRef , AnyRef]  
{
	
	override def act(taskMover: akka.actor.ActorRef ) {
		tasks map ( (x)=> { x.setInput(this.input); taskMover ! x })
		output = "done"
	}

}

