/* Using giVE.flow import external content and create output for graphml or any other format 
Right now using myexperiments open dataset as example, todo: move to a separate project

*/
package org.give.app
import scala.xml._
import org.give 

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import java.util.Date

import org.give.flow.mover.TasksMover

import org.give.flow.tasks._
import org.give.flow.tasks.util._
import org.give.imports.tasks.myexperiments._

object giVE_app extends App { 

	println("giVE_app")
	 
	
 	val _system = ActorSystem("giVE")

 	val tasksMover = _system.actorOf( Props[TasksMover], name= "tasksMover" );

 	var getUsersTask:TaskBase = null
	//download all users and their detailed XMLs, and process them, use Loopback to fetch all users until there
	//are none returned
 	getUsersTask = DownloadURLTask( 
 						specName = "UsersDownload", url="http://www.myexperiment.org/users.xml?num=25", 
 						saveToFile = true,
 						page = 1 , 
						nextTask = XmlParseTask( 
										specName = "Parse XML",
										nextTask =  IterateUsersTask( 
														specName="Iterate Users", 
														taskRunner = tasksMover,
														nextTask = 
														LoopbackTask ( backToTask  = { ()=> getUsersTask } , 
																loopUntil = {  (mySelf)  =>   
																	!mySelf.input.asInstanceOf[ Seq[String] ].isEmpty  
																} 
																,
																endTask = GraphExportEnd() ) 
													)  

									).waitForNext()
					
					)
 	// run
	tasksMover ! getUsersTask

	// todo wait for all URLs to be processed, depends on flow to support Future
	readLine
	GraphMLStreams.flushAllStreams
	_system.shutdown
}


case  class GraphExportEnd ( override val specName:String = "GraphExportEnd")  extends Task[ AnyRef , AnyRef]  
{
	
	override def act(taskMover: akka.actor.ActorRef ) {
		println ("Flush GraphML Files")
		GraphMLStreams.flushAllStreams
		output = "done"
	}

}
