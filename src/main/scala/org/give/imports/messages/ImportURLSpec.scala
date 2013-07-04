package org.give.imports.messages
import java.util.Date

abstract class ActorMessageState( val whenStarted: Date  = new Date, var whenProcessed:Date = null)
case class Requested extends ActorMessageState 
case class Processed extends ActorMessageState 
case class Failed extends ActorMessageState 
 
abstract class ActorSpec( 
	var specName: String = "",
	var state: ActorMessageState = Requested(), 
	var nextSpec: ActorSpec = null,
	var success:Boolean = false , 
	var message: String = "" ,
	var input: String ="",
	var output: String = ""
) {
	def act(replyTo: akka.actor.ActorRef) = {}
}

case class ImportURLSpec ( val name: String , val url: String ) extends ActorSpec  {

	import scala.xml._
	import scala.io._
	import scala.concurrent._
	//import context.dispatcher
	import java.util.Date
	import java.net.URL
	import java.nio.charset._

	import ExecutionContext.Implicits.global

	// using Tomasz Nurkiewicz  async approach
	def downloadPage(url: String) = Future {
	    scala.io.Source.fromURL(new URL( url ) ).mkString
	}
	 
	def parseXML(xmlString: String) =   {
	    //println(xmlString)
	    XML.loadString(xmlString)
	    
	}

	def parseName(xElem: Elem) =   Future { 
		xElem  \ "name"  text 
	}

/*
<xtl>
        <fetch  url="users">
	<foreach  select="user">
		<fetch url="user?id{abc}"  save="">
			<fetch url="picture?id{abc}" save="" />
			<fetch url="groups?lid{abc}"  save=""/>
			<transform>
				<graphml>


				</graphml>
			</transform>

		</fetch>
	</foreach>
        </fetch>

        <fetch  url="workflows">
	<foreach  select="workflow">
		<fetch url="workflow?id{abc}" >
			<fetch url="workfowXML?id{abc}" />
			<fetch url="users?lid{abc}" />
			

		</fetch>
	</foreach>
        </fetch>
</xtl>


*/



	input = url
	specName = name
	override def act( replyTo: akka.actor.ActorRef) = { System.out.println( " Acting ImportURLSpec " + input ); 



	 	val xmlStringFuture: Future[String] = downloadPage ( url )
	 	val xmlDocFuture : Future[Elem] = xmlStringFuture map parseXML
	 	val getNameFuture: Future[String] = xmlDocFuture flatMap parseName

	 	//getNameFuture foreach println
	 	getNameFuture  onFailure    {
	 				case t => {  	
	 					println("onFailure xmlStringFuture ")
	 					success = false
	 					message = t.getMessage
	 					state = Processed()  // todo use Failed as another state
	 					state.whenProcessed = new Date 

	 					replyTo ! this
	 				}
	 			} 
	 	getNameFuture  onSuccess   {
	 				case result => { 
			 			println("onSuccess  parseName ")

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


case class ProcessUserSpec ( val name: String ) extends ActorSpec  {
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
