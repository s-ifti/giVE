package org.give.app
import scala.xml._
import org.give 

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout


import org.give.imports.actors._

import org.give.imports.messages._

/* General Repeater
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

case class IterateUsersTask( val name: String ,  val taskRunner: akka.actor.ActorRef ) extends ActorTask[Elem, Seq[String] ] {
	specName = name
	override def act( replyTo: akka.actor.ActorRef) = { 
		//println  " Acting IterateUsersTask "
		var urlUsers: Seq[String] =   input  \\ "user" map(x=> x.attribute("uri").get toString)
		
		output = urlUsers

		urlUsers.foreach( userURI =>   { 
			var urlUserDownloadTask = DownloadURLTask(  name = "Download", url= userURI ) 
			urlUserDownloadTask.nextSpec = XmlParseTask( name = "Parse XML") 
			urlUserDownloadTask.nextSpec.nextSpec = ParseNameTask( name = "Parse Name")
			urlUserDownloadTask.nextSpec.nextSpec.nextSpec = ProcessUserSpec( name = "Convert User to GraphML " )
		                      
		 	taskRunner  !  urlUserDownloadTask;
 		} )


	}

}

 object giVE_app extends App { 

	println("giVE_app")
	def iter(head:ActorTaskBase, tail: List[ActorTaskBase]):AnyRef = {   
		
		println( "HEAD: " + head )

		if  ( ! tail.isEmpty ) {
			iter( tail.head, tail.tail ); 
		}
		return null;
	}
	
 	val _system = ActorSystem("giVE")

 	val tasks = _system.actorOf( Props[TasksTracker], name= "tasksTracker" );

	/* val urlTask = DownloadURLTask(  name = "Download", url="http://www.myexperiment.org/user.xml?id=23" ) 
	urlTask.nextSpec = XmlParseTask( name = "Parse XML") 
	urlTask.nextSpec.nextSpec = ParseNameTask( name = "Parse Name")
	urlTask.nextSpec.nextSpec.nextSpec = ProcessUserSpec( name = "Convert User to GraphML " )
                      
 	tasks  !  urlTask;
	*/

	//download all users and their detailed XMLs
 	val getUsers = DownloadURLTask( name = "Download", url="http://www.myexperiment.org/users.xml" ) 
	getUsers.nextSpec = XmlParseTask( name = "Parse XML") 
	getUsers.nextSpec.nextSpec = IterateUsersTask( name="Iterate Users", tasks)

	tasks ! getUsers

	Thread.sleep(5000)	
	//println("link: " + urlTask.getOutput + " xml: " +  urlTask.nextSpec.getOutput + " name:" + urlTask.nextSpec.nextSpec.getOutput )
	iter( TasksTracker.results.head, TasksTracker.results.tail );
	_system.shutdown
}

