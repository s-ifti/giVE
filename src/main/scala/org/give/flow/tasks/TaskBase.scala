/*
# Flow Tasks abstractions
*/
package org.give.flow.tasks

import java.util.Date



abstract class TaskState( val whenStarted: Date  = new Date, var whenProcessed:Date = null)

case class Requested extends TaskState 
case class Processed extends TaskState 
case class Failed extends TaskState 

/*
### TaskBase
This represent base Task abstraction for giVE.Flow framework
*/
abstract class TaskBase (
	/* 
		#### Members (immutable)
	*/
	val specName: String = "",
	/*
		when task was created
	*/
	val whenCreated: Date = new Date,
	/* 
		#### Members (mutable)
	*/
	/*
		when task was received for processing by TasksMover
	*/
	var whenRecieved: Date  = null,
	/*
		nextTask for flow (TasksMover will use it to pass output to nextTask input, thus creating a pipe) 
	*/
	val nextTask: TaskBase = null,
	/*
		any message by task once it is finished (e.g. error in case of an error)
	*/
	var message: String = "",
	/*
		#### Members (Private)
	*/
	private var state: TaskState = Requested(), 
	private var success:Boolean = false )  extends Cloneable 
{ 

	def getInput() : AnyRef {}
	def setInput( input:AnyRef ) {} 
	def getOutput() : AnyRef {}
	def setOutput( output : AnyRef ) {}
	def getNextTask():TaskBase = {
		return nextTask;
	}
	def isSuccess(): Boolean = { return success }
	def getState(): TaskState = { return state }

	/* primary method that is overriden by individual task to perform an action
		each task responsibility is to complete a task and reply back a cloned Processed state representation of
		them to replyTo
		replyTo can also be used to send further tasks to TasksMover allowing complex
		async chains to be developed
	*/
	def act(replyTo: akka.actor.ActorRef) = {}
 	
 	/*
 		#### returns cloned representation of self with state changed to Processed
 		Keeping state constants allow easy loops and reuse of tasks specification
 	*/
 	def processed( r_success : Boolean, r_message: String , r_output: AnyRef ): TaskBase  = {
 		var cloned: TaskBase = this.clone().asInstanceOf[ TaskBase ]
 		cloned.success = r_success
 		cloned.message = r_message
 		cloned.setOutput ( r_output )

		cloned.state = Processed()
		cloned.state.whenProcessed = new Date 
		
 		return cloned

 	}
 	var observerTasks: List[TaskBase] = List()

 	def addObserver(me:TaskBase):TaskBase = { 
 		observerTasks = me :: observerTasks 
 		return me
 	}
 	def waitFor(me:TaskBase):TaskBase = {
 		waitingForTasks = me :: waitingForTasks
 		me.addObserver( this )
 		return me
 	}
 	var waitingForTasks: List[TaskBase] = List()

 	def allWaitOver = {}
 	def observedTaskDone(replyTo: akka.actor.ActorRef, whoIsDone:TaskBase):Boolean = {
 		//println("observableTaskDone " + whoIsDone.specName + " for " + this.specName)
 		waitingForTasks = waitingForTasks.filter( (x) => x.specName != whoIsDone.specName )   
 		if(waitingForTasks.isEmpty ) {

 			observerTasks.foreach(x => { x.observedTaskDone(replyTo, this) })
 			observerTasks = List()
 			allWaitOver
 			return true;
 		}
		return false;
	}
	def waitForNext():TaskBase = {
		waitFor( nextTask )
		return this
	}
 	/* todo move to trait TaskCanRepeat */
 	def loopIncrement(): Int = { 0 }
 	def loopIndex(): Int = { 0 }

}

/*
### Task provide a generic implementation of input and output, enabling
simpler DSL like creation of task flow specification

*/
abstract class Task[INPUT, OUTPUT] ( 
	var input: INPUT = null,
	var output: OUTPUT = null)  extends TaskBase 
{

	 
	
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

	/* given an input process it and generate output
	*/
	override def act(replyTo: akka.actor.ActorRef) = {}

	
}

