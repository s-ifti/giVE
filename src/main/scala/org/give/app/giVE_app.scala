package org.give.app
import scala.xml._
import org.give 

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import java.util.Date

import org.give.imports.actors._

import org.give.imports.messages._

/* General Repeater todo
case class RepaterTask( val name: String, innerTasks : List[ActorTaskBase]  ) extends  ActorTask[AnyRef,AnyRef]{
	specName = name


	override def act( replyTo: akka.actor.ActorRef) = { 
		//println  " Acting RepeaterTask "   
		var idx: Int = 0
		

		innerTasks.foreach( x => {
				x.setInput (  this.getInput )
				x.inputIndex = idx+ 1  
				x.act( replyTo )
				idx = idx + 1
			});

	}
}
*/

case class IterateUsersTask( override val specName: String ,  val taskRunner: akka.actor.ActorRef ) extends ActorTask[Elem, Seq[String] ] {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		//println  " Acting IterateUsersTask "
		var urlUsers: Seq[String] =   input  \\ "user" map(x=> x.attribute("uri").get toString)
		
		output = urlUsers

		urlUsers.foreach( userURI =>   { 
			var urlUserDownloadTask = DownloadURLTask(  specName = "Download", url= userURI ) 
			urlUserDownloadTask.nextSpec = XmlParseTask(  specName = "Parse XML") 
			urlUserDownloadTask.nextSpec.nextSpec = ParseNameTask( specName = "Parse Name")
			urlUserDownloadTask.nextSpec.nextSpec.nextSpec = ProcessUserSpec( specName = "Convert User to GraphML " )
		                      
		 	taskRunner  !  urlUserDownloadTask;


 		} )
 		// need to support Future interface within ActorTask so we can wait for all to finish
 		taskRunner ! processed(  true, "processing total " + urlUsers.count( (x)=> { true } ) + " users" ,  urlUsers )


	}

}
case  class LoopbackTask ( Task : DownloadURLTask  ) extends ActorTask[ Seq[String] , String]  
{
	override def act(taskRunner: akka.actor.ActorRef ) {

		println("LoopbackTask")
		// don't we need mutual exclusion here here

		Task.page = Task.page + 1
		var iteration = Task.page


		if(  !  this.input.isEmpty  ) {
			println( "iteration # " + iteration)
			output = "loop"
			
			taskRunner ! Task
			taskRunner ! processed(  true, "loop # " +  iteration, "end" )

		}
		else {
			output = "end"
			println ("No more users found !")
			taskRunner ! processed(  true, "no more users", "end" )
		}
	}

}
 object giVE_app extends App { 

	println("giVE_app")
	 
	
 	val _system = ActorSystem("giVE")

 	val tasks = _system.actorOf( Props[TasksTracker], name= "tasksTracker" );

	/* val urlTask = DownloadURLTask(  name = "Download", url="http://www.myexperiment.org/user.xml?id=23" ) 
	urlTask.nextSpec = XmlParseTask( name = "Parse XML") 
	urlTask.nextSpec.nextSpec = ParseNameTask( name = "Parse Name")
	urlTask.nextSpec.nextSpec.nextSpec = ProcessUserSpec( name = "Convert User to GraphML " )
                      
 	tasks  !  urlTask;
	*/

	//download all users and their detailed XMLs
 	val getUsers = DownloadURLTask( specName = "Download", url="http://www.myexperiment.org/users.xml", page = 1 ) 
	getUsers.nextSpec = XmlParseTask( specName = "Parse XML") 
	getUsers.nextSpec.nextSpec = IterateUsersTask( specName="Iterate Users", tasks)
	getUsers.nextSpec.nextSpec.nextSpec  = LoopbackTask ( Task  = getUsers   )
	tasks ! getUsers

	//Thread.sleep(25000)	
	readLine
	//TasksTracker.results map println

	_system.shutdown
}

