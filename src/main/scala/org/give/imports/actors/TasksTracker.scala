package org.give.imports.actors

import akka.actor.Actor
import akka.pattern.AskSupport

import org.give.imports.messages._
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import akka.routing.RoundRobinRouter;


class TasksTracker extends Actor {
	import context._
	import java.util.Date

	/* val downloader = context.actorOf(
        		 Props[DownloadResourceListActor].withRouter(new RoundRobinRouter(5)),    "downloader");
*/
	
	def receive: Receive = { 

		case  msg:  ActorSpec => {
			println("Received message " + msg )
			msg.state match{
			  	case  _ : Processed => { 
			  		println ( "Processed  " + msg.output )
		 			TasksTracker.results = msg :: TasksTracker.results

		 			if( msg.nextSpec != null ) {
		 				msg.nextSpec.input = msg.output
		 				self ! msg.nextSpec
		 			}

		 		}
		 		case _ : Requested => {
			  		println ( "Requested")
		 			TasksTracker.requests = msg :: TasksTracker.requests
		 			msg.state.whenProcessed = new Date
		 			msg.act( self )
		 			//downloader   !  msg
		 		}
		 	}
		 }

	}
}

object TasksTracker {
	
	var results : List[ActorSpec] = List()
	var requests : List[ActorSpec] = List()
}
