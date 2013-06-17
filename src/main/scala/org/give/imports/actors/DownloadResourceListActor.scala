package org.give.imports.actors

import akka.actor.Actor
import akka.pattern.AskSupport

import org.give.imports.messages._


import scala.xml._
import scala.io._
import scala.concurrent._
import context.dispatcher
import java.util.Date
import java.net.URL
import java.nio.charset._




class DownloadResourceListActor extends Actor  with AskSupport{
	 

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

	def receive: Receive = { 
		
		case  spec: ImportURLSpec => { 

		//stash sender so it is avaialble in async i/o response
		val replyTo = sender

	 	val xmlStringFuture: Future[String] = downloadPage ( spec.url )
	 	val xmlDocFuture : Future[Elem] = xmlStringFuture map parseXML
	 	val getNameFuture: Future[String] = xmlDocFuture flatMap parseName

	 	//getNameFuture foreach println
	 	getNameFuture  onFailure    {
	 				case t => {  	
//							println("onFailure xmlStringFuture ")
	 					spec.success = false
	 					spec.message = t.getMessage
	 					spec.state = Processed()  // todo use Failed as another state
	 					spec.state.whenProcessed = new Date 

	 					replyTo ! spec
	 				}
	 			} 
	 	getNameFuture  onSuccess   {
	 				case result => { 
//		 					println("onSuccess  parseName ")

	 					spec.success = true
	 					spec.message = result
	 					spec.state = Processed()
		 				spec.state.whenProcessed = new Date 

		 				replyTo! spec

	 				}
	 			} 
	 			 

		 	
		 	
		 }

	}
}
