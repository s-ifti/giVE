/*
myexperiments.org specific tasks, e.g. fetching user content

*/
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



case class IterateUsersTask( override val specName: String ,  val taskRunner: akka.actor.ActorRef 
	, override val nextTask: TaskBase = null
) extends Task[Elem, Seq[String] ] {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		//println  " Acting IterateUsersTask "
		var urlUsers: Seq[String] =   input  \\ "user" map(x=> { x.attribute("uri").get.toString } )
		
		output = urlUsers

		urlUsers.foreach( userURI =>   { 
			var urlUserDownloadTask = DownloadURLTask(  
										specName = "Download", 
										url= userURI + "&all_elements=yes", 
										nextTask = XmlParseTask(  
														specName = "Parse XML",
														nextTask = CompositeTasks( specName="composite process user",
																				tasks = List(
																							ProcessUserNode ( 
																								specName = "Process user node " ,
																								nextTask = PrintInput()
																							),
																							ProcessUserEdges ( 
																								specName = "Process user edges" ,
																								nextTask = PrintInput()
																							)
																						)
																	)
													)
										)
		                      
		 	taskRunner  !  urlUserDownloadTask;


 		} )
 		// need to support Future interface within Task so we can wait for all to finish
 		taskRunner ! processed(  true, "processing total " + urlUsers.count( (x)=> { true } ) + " users" ,  urlUsers )


	}

}


case class ProcessUserNode ( override val specName: String 
	,  override val nextTask: TaskBase = null
) extends Task[Elem, Elem ]  {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		var name:String = input \ "name" text
		var id:String = input \ "id" text
		var uri:String = input.attribute("uri").get.toString
		var node = 
				<node id={uri}>
					<data key="uri">{uri}</data>
					<data key="type">myexperiments.org/user</data>
					<data key="name">{name}</data>
				</node>
		replyTo ! processed( true, "user node processed", node )
	}
}

case class ProcessUserEdges ( override val specName: String 
	,  override val nextTask: TaskBase = null
) extends Task[Elem, Seq[Elem] ]  {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		var friendsURI: Seq[String] =  input  \\ "friend" map(x=> { x.attribute("uri").get.toString })
		
		var myuri:String = input.attribute("uri").get.toString
		ProcessUserEdges._edgeCtr = ProcessUserEdges._edgeCtr + 1
		val edgeCtr:String = ProcessUserEdges._edgeCtr.toString
		var allEdges: List[Elem] = List()

		friendsURI map ( (x) =>  {
				val thisEdge = 
						<edge id={edgeCtr} label="myfriend" source={myuri} target={x}>
						</edge>
			 	allEdges = thisEdge::allEdges 
			} )
		
		replyTo ! processed( true, "user edges processed", allEdges )
	}
}
object ProcessUserEdges {

	var _edgeCtr: Int = 0
}


