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
		#### Members with One time initializations
	*/
	val specName: String = "",
	val whenCreated: Date = new Date,
	/* 
		#### Members that can be changed
	*/
	var whenRecieved: Date  = null,
	var nextSpec: TaskBase = null,
	var message: String = "",
	/*
		#### Private
	*/
	private var state: TaskState = Requested(), 
	private var success:Boolean = false )  extends Cloneable 
{ 

	def getInput() : AnyRef {}
	def setInput( input:AnyRef ) {} 
	def getOutput() : AnyRef {}
	def setOutput( output : AnyRef ) {}
	def getNextTask(): TaskBase {}
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

 	/* move to trait TaskCanRepeat */
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


	override def getNextTask():TaskBase = {
		return nextSpec.asInstanceOf[ TaskBase ];
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

