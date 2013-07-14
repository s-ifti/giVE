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
### akka exceutioncontext global
*/

import ExecutionContext.Implicits.global
/* 
## Given a URL retrieve it and generate string output 
*/
case class DownloadURLTask ( override val specName: String , val url: String, var page:Int = 1 ) extends Task[String, String]  
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
		if ( page > 1  )
			fetchURL += ("?page=" + page.toString() )  
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
case class XmlParseTask ( override val specName: String ) extends Task[String, Elem ]  
{
	def parseXML(xmlString: String):Elem =   {


	    XML.loadString(xmlString)
	    
	}

	override def act( replyTo: akka.actor.ActorRef) = { 
		//System.out.println( " Acting XmlParseTask " + input ); 


	 	val userXml: Elem =  parseXML( input )
		//println("onSuccess  xmlDocFuture ")
		replyTo ! processed(  true, "xml loaded" , userXml )



	}
}


/* given an XML document find an element */

case class ParseNameTask( override val specName: String , val elementName : String, val anyDescendants: Boolean = false ) extends Task[Elem, String]  {

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
	var task : TaskBase , 
	var loopUntil: ( Task[AnyRef,AnyRef] ) => Boolean   ) extends Task[ AnyRef , AnyRef]  
{
	
	override def act(taskMover: akka.actor.ActorRef ) {

		println("LoopbackTask")
		// don't we need mutual exclusion here here
		val iteration = task.loopIncrement()
		//Task.page = Task.page + 1
		//var iteration = Task.page


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
