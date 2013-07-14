/*
# Tasks Mover 
*/
package org.give.flow.mover

/*
## Dependencies
### akka Actor
*/

import akka.actor.Actor
import akka.pattern.AskSupport

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedActor
import akka.routing.RoundRobinRouter


/*
### flow tasks
*/
import org.give.flow.tasks._




class TasksMover extends Actor {
	import context._
	import java.util.Date

	def receive: Receive = { 

		case  msg:  TaskBase=> {
			//println("Received message " + msg )
			msg.getState() match{
			  	case  _ : Processed => { 
			  		//println ( "Processed  " + msg.specName + " message: " + msg.message)
		 			msg.whenRecieved = new Date
		 			TasksMover.results = msg :: TasksMover.results
		 			var nextTask = msg.getNextTask
		 			if( msg.isSuccess && nextTask  != null ) {
		 				nextTask.setInput ( msg.getOutput )
		 				
		 				self ! nextTask
		 			}
		 			else if( !msg.isSuccess ) {
		 				println ("task " + msg.specName + " failed. message:" + msg.message )
		 			}

		 		}
		 		case _ : Requested => {
			  		//println ( "Requested " + msg.specName)
		 			msg.whenRecieved = new Date
		 			TasksMover.requests = msg :: TasksMover.requests
		 			msg.act( self )
		 			//downloader   !  msg
		 		}
		 	}
		 }

	}
}

object TasksMover {
	var results : List[TaskBase] = List()
	var requests : List[TaskBase] = List()
}
