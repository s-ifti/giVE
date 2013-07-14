package org.give.imports.tasks.myexperiments
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

case class IterateUsersTask( override val specName: String ,  val taskRunner: akka.actor.ActorRef ) extends Task[Elem, Seq[String] ] {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		//println  " Acting IterateUsersTask "
		var urlUsers: Seq[String] =   input  \\ "user" map(x=> x.attribute("uri").get toString)
		
		output = urlUsers

		urlUsers.foreach( userURI =>   { 
			var urlUserDownloadTask = DownloadURLTask(  specName = "Download", url= userURI ) 
			urlUserDownloadTask.nextSpec = XmlParseTask(  specName = "Parse XML") 
			urlUserDownloadTask.nextSpec.nextSpec = ParseNameTask( specName = "Parse Name", elementName = "name")
			urlUserDownloadTask.nextSpec.nextSpec.nextSpec = ProcessUserSpec( specName = "Convert User to GraphML " )
		                      
		 	taskRunner  !  urlUserDownloadTask;


 		} )
 		// need to support Future interface within Task so we can wait for all to finish
 		taskRunner ! processed(  true, "processing total " + urlUsers.count( (x)=> { true } ) + " users" ,  urlUsers )


	}

}



case class ProcessUserSpec ( override val specName: String ) extends Task[String, String]  {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		//System.out.println( " Acting ProcessUserSpec " + input ); 
		output = "ALL HAIL TO " + input;
		replyTo ! processed(  true, "transformed" , "ALL HAIL TO " + input )
	}
}
