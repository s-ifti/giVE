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

		case  msg:  ActorTaskBase=> {
			println("Received message " + msg )
			msg.state match{
			  	case  _ : Processed => { 
			  		println ( "Processed  " + msg.specName )
		 			TasksTracker.results = msg :: TasksTracker.results
		 			var nextTask = msg.getNextTask
		 			if( nextTask  != null ) {
		 				nextTask.setInput ( msg.getOutput )
		 				
		 				self ! nextTask
		 			}

		 		}
		 		case _ : Requested => {
			  		println ( "Requested " + msg.specName)
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
	
	var results : List[ActorTaskBase] = List()
	var requests : List[ActorTaskBase] = List()
}
