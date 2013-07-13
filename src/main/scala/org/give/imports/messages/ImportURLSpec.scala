package org.give.imports.messages
import java.util.Date

import scala.xml._
import scala.io._
import scala.concurrent._
import java.nio.charset._
import java.net.URL

import ExecutionContext.Implicits.global

abstract class ActorMessageState( val whenStarted: Date  = new Date, var whenProcessed:Date = null)
case class Requested extends ActorMessageState 
case class Processed extends ActorMessageState 
case class Failed extends ActorMessageState 

abstract class ActorTaskBase (
	val specName: String = "",
	var nextSpec: ActorTaskBase = null,
	private var state: ActorMessageState = Requested(), 
	private var success:Boolean = false , 
	private var message: String = "" ,
	var whenRecieved: Date  = null,
	var inputIndex:Int = 0 )  extends Cloneable 
{ 

	def getInput() : AnyRef {}
	def setInput( input:AnyRef ) {} 
	def getOutput() : AnyRef {}
	def setOutput( output : AnyRef ) {}
	def getNextTask(): ActorTaskBase {}
	def isSuccess(): Boolean = { return success }
	def getState(): ActorMessageState = { return state }
 	def act(replyTo: akka.actor.ActorRef) = {}
 	
 	def processed( r_success : Boolean, r_message: String , r_output: AnyRef ): ActorTaskBase  = {
 		var cloned: ActorTaskBase = this.clone().asInstanceOf[ ActorTaskBase ]
 		cloned.success = r_success
 		cloned.message = r_message
 		cloned.setOutput ( r_output )

		cloned.state = Processed()
		cloned.state.whenProcessed = new Date 

 		return cloned

 	}
}

abstract class ActorTask[INPUT, OUTPUT] ( 
	var input: INPUT = null,
	var output: OUTPUT = null)  extends ActorTaskBase 
{


	override def getNextTask():ActorTaskBase = {
		return nextSpec.asInstanceOf[ ActorTaskBase ];
	}
	override def getInput(): AnyRef = {
		return input.asInstanceOf[ AnyRef ]
	}
	override def setInput( inp:  AnyRef ) = {
		input = inp.asInstanceOf[ INPUT ]
	} 
	override def getOutput() :  AnyRef  = {
		return  output .asInstanceOf[ AnyRef  ]
	}
	
	override def setOutput( out:  AnyRef ) = {
		output = out.asInstanceOf[ OUTPUT  ]
	} 
	override def act(replyTo: akka.actor.ActorRef) = {}

	
}

case class DownloadURLTask ( override val specName: String , val url: String, var page:Int = 1 ) extends ActorTask[String, String]  
{
	 
	input = url
//	specName = name

	
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


case class XmlParseTask ( override val specName: String ) extends ActorTask[String, Elem ]  
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



case class ParseNameTask( override val specName: String ) extends ActorTask[Elem, String]  {

	 def parseName(xElem: Elem) =   { 
		xElem  \ "name"  text 
	}
	override def act( replyTo: akka.actor.ActorRef) = { 
	 	val  userName = parseName( input )
		replyTo ! processed(  true, "parsed" , userName)
	}
}

case class ProcessUserSpec ( override val specName: String ) extends ActorTask[String, String]  {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		//System.out.println( " Acting ProcessUserSpec " + input ); 
		output = "ALL HAIL TO " + input;
		replyTo ! processed(  true, "transformed" , "ALL HAIL TO " + input )
	}
}
