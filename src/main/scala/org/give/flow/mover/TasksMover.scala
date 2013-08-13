/*
# Tasks Mover 
This is the main execution Actor (using akka)
The approach is to build chain of tasks that can be executed asynchronously (but with a specific PIPE oriented order)
by an actor 
This is similar to TinkerPop Pipe abstraction, but is a custom implementation
The framework allows combining various crawl and extract actions and chaining them to complete an import/export
objective, e.g. this can be used to import external dataset to be imported in a certain format, as used in
myexperiments example.
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

		 		case _ : Requested => {
		 			/* All tasks start with Requested state, at this state TasksMover will call
		 				method act on them, tasks can then process input and generate an
		 				output, they will also mark their state as success or failure, and will then
		 				generate another message to TasksMover with same object cloned with state now
		 				set to Processed, at that stage the task can be inspected to see if there is
		 				any nextTask in chain, which then is again sent back to TasksMover (assumption is made
		 				that state was immutable, so task state was kept in Requested state )
		 				At any time TasksMover can receive further tasks from individual act method of a Task
		 			*/
			  		//println ( "Requested " + msg.specName)
		 			msg.whenRecieved = new Date
		 			//track requests in a central place
		 			TasksMover.requests = msg :: TasksMover.requests
		 			msg.act( self )
		 			
		 		}

			  	case  _ : Processed => { 
			  		/* Processed is to be sent when a task has finished its work
			  			This enable checking to see if there is nextTask in chain and input is then
			  			passed to it and is send to tasksMover for processing
			  		*/
			  		//println ( "Processed  " + msg.specName + " message: " + msg.message)
		 			msg.whenRecieved = new Date
		 			//track reslts in a central place
		 			TasksMover.results = msg :: TasksMover.results
		 			var nextTask = msg.getNextTask
		 			if( msg.isSuccess && nextTask  != null ) {
		 				// todo: review implicit observer
		 				//nextTask.addObserver(msg)
		 				nextTask.setInput ( msg.getOutput )
		 				self ! nextTask
		 			}
		 			else if( !msg.isSuccess ) {
		 				println ("task " + msg.specName + " failed. message:" + msg.message )
		 			}
	 				if( msg.isSuccess && !msg.observerTasks.isEmpty  ) {
	 					msg.observerTasks.foreach(x => { x.observedTaskDone(self, msg) })
	 				}


		 		}		 	
		 	}
		 }

	}
}

object TasksMover {
	var results : List[TaskBase] = List()
	var requests : List[TaskBase] = List()
}
