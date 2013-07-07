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

abstract class ActorTaskBase(
	var specName: String = "",
	var nextSpec: ActorTaskBase = null,
	var state: ActorMessageState = Requested(), 
	var success:Boolean = false , 
	var message: String = "" ,
	var inputIndex:Int = 0 )  { 

	def getInput() : AnyRef {}
	def setInput( input:AnyRef ) {} 
	def getOutput() : AnyRef {}
	def getNextTask(): ActorTaskBase {}
 	def act(replyTo: akka.actor.ActorRef) = {}

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
	
	override def act(replyTo: akka.actor.ActorRef) = {}
}

case class DownloadURLTask ( val name: String , val url: String ) extends ActorTask[String, String]  
{
	 
	input = url
	specName = name


	// using Tomasz Nurkiewicz  async approach
	def downloadPage(url: String) = Future {
	    scala.io.Source.fromURL(new URL( url ) ).mkString
	}
	override def act( replyTo: akka.actor.ActorRef) = { System.out.println( "Task DownloadURLTask" + input ); 



	 	val downloadStringFuture: Future[String] = downloadPage ( url )

	 	downloadStringFuture  onFailure    {
	 				case t => {  	
	 					println("onFailure downloadStringFuture ")
	 					success = false
	 					message = t.getMessage
	 					state = Processed()  // todo use Failed as another state
	 					state.whenProcessed = new Date 

	 					replyTo ! this
	 				}
	 			} 
	 	downloadStringFuture  onSuccess   {
	 				case result => { 
			 			println("onSuccess  downloadStringFuture ")

	 					success = true
	 					message = "downloaded"
	 					output = result
	 					state = Processed()
		 				state.whenProcessed = new Date 

		 				replyTo! this

	 				}
	 			} 

	}
}


case class XmlParseTask ( val name: String ) extends ActorTask[String, Elem ]  
{
	specName = name



	 
	def parseXML(xmlString: String):Elem =   {


	    XML.loadString(xmlString)
	    
	}

	override def act( replyTo: akka.actor.ActorRef) = { System.out.println( " Acting XmlParseTask " + input ); 


	 	val userXml: Elem =  parseXML( input )
		println("onSuccess  xmlDocFuture ")

		success = true
		message = "xml loaded"
		output = userXml
		state = Processed()
		state.whenProcessed = new Date 

		replyTo! this


	}
}



case class ParseNameTask( val name: String ) extends ActorTask[Elem, String]  {
	specName = name


	 def parseName(xElem: Elem) =   { 
		xElem  \ "name"  text 
	}

	override def act( replyTo: akka.actor.ActorRef) = { System.out.println( " Acting ImportURLSpec " + input ); 


	 	val  userName = parseName( input )

		println("onSuccess  XmlParseName ")

		success = true
		message = "parsed"
		output = userName
		state = Processed()
		state.whenProcessed = new Date 

		replyTo! this

	}
}

case class ProcessUserSpec ( val name: String ) extends ActorTask[String, String]  {
	specName = name
	override def act( replyTo: akka.actor.ActorRef) = { System.out.println( " Acting ProcessUserSpec " + input ); 
		output = "ALL HAIL TO " + input;

		success = true
		message = "transformed"
		state = Processed()
		state.whenProcessed = new Date 

		replyTo! this
	}
}
