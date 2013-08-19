/*
myexperiments.org specific tasks, e.g. fetching user content
TODO: move to specific project for myexperiments
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
import java.io._
import org.give.flow.mover.TasksMover

import org.give.flow.tasks._
import org.give.flow.tasks.util._

case class AccumulateNodes (  var nodesFileName:String 
) extends Task[Elem, String ]  {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		GraphMLStreams.writeNode(nodesFileName, input.toString())

		replyTo ! processed( true, "stored", "stored" )
	}
}
case class AccumulateEdges (   var edgesFileName:String 
) extends Task[ Seq[Elem], String ]  {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		
		input.foreach( edge =>   { 
					GraphMLStreams.writeEdges(edgesFileName, edge.toString())
				} )
		replyTo ! processed( true, "stored", "stored" )
	}
}


object GraphMLStreams {
	var _streams:Map[String, PrintWriter] = Map()

	

	def writeNode(fileName:String, x:String) = {

		//need lock to avoid mingled stream output, todo any optimization for concurrency
		_streams.synchronized { 
			var nodesWriter:PrintWriter = null
			//todo synchronize, use immutable map, see if assignment to _streams is synchronized
			if( !_streams.contains(fileName)) {
				nodesWriter = new PrintWriter(new File(fileName + ".xml")) 
				_streams += (fileName -> nodesWriter )
				writeMetadataElements(nodesWriter)
			}
			else {
				nodesWriter = _streams(fileName)
			}
			nodesWriter.write(x)
			nodesWriter.write("\n")
		}

	}


	def writeEdges(fileName:String, x:String) = {
		//need lock to avoid mingled stream output, todo any optimization for concurrency
		_streams.synchronized { 
			var edgesWriter: PrintWriter = null
			//todo synchronize, use immutable map, see if assignment to _streams is synchronized
			if( !_streams.contains(fileName)) {

				edgesWriter = new PrintWriter(new File(fileName + ".xml")) 
				_streams += (fileName -> edgesWriter )
				writeMetadataElements(edgesWriter)

			}
			else {
				edgesWriter = _streams(fileName)
			}
			edgesWriter.write(x)
			edgesWriter.write("\n")
		}
	}
	def writeMetadataElements(writer:PrintWriter ) = {
		writer.write("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">");
			writeGraphDef(writer, "uri", "node", "string")
			writeGraphDef(writer, "name", "node", "string")
			writeGraphDef(writer, "type", "node", "string")
			writeGraphDef(writer, "content", "node", "string")
			writeGraphDef(writer, "resourcePictureURL", "node", "string")
			writeGraphDef(writer, "valueString", "node", "string")
			writeGraphDef(writer, "createdDate", "node", "datetime")
			writeGraphDef(writer, "byUser", "node", "string")
			writeGraphDef(writer, "edgetype", "edge", "string")
			writeGraphDef(writer, "weight", "edge", "int")
 			writer.write( "<graph id=\"G\" edgedefault=\"directed\">\n" )
	}
	def writeEndElements(writer:PrintWriter) = {
			writer.write("</graph>\n</graphml>\n")
	}
	def flushAllStreams = {
		println("flushAllStreams")
				//need lock to avoid mingled stream output, todo any optimization for concurrency
		_streams.synchronized { 
			_streams.values.foreach(x => { 
				writeEndElements(x)
			 	x.flush()
			 	x.close() 
			})
			//reset streams as concurrent calls can come
			_streams = Map()
		}
	}

	def writeGraphDef(writer:PrintWriter , id:String, forWhat:String = "node", datatype:String  = "string") = {
        	writer.write("<key id=\""+ id + "\" for=\""+ forWhat + "\" attr.name=\"" + id + "\" attr.type=\"" + datatype + "\" />")
        	writer.write("\n")

    }

}


case class IterateUsersTask( override val specName: String ,  val taskRunner: akka.actor.ActorRef 
	, override val nextTask: TaskBase = null
) extends Task[Elem, Seq[String] ] {
	var urlUsers: Seq[String] = null

	override def observedTaskDone(replyTo: akka.actor.ActorRef, who:TaskBase ):Boolean = {
		var ret = super.observedTaskDone(replyTo, who)
		if( ret ) {
			//println("all Users download done for " + who.specName )
			replyTo ! processed( true, "Total " + urlUsers.length + " processed !",  urlUsers)
		}
		else {
			println(". " )
		}
		return ret
	}
	override def act( replyTo: akka.actor.ActorRef) = { 
		//println  " Acting IterateUsersTask "
		urlUsers =   input  \\ "user" map(x=> { x.attribute("uri").get.toString } )
		
		output = urlUsers
		if ( urlUsers == null || urlUsers.length == 0 ) {


			replyTo ! processed( true, "No Users returned ",  urlUsers)
		}

		urlUsers.foreach( userURI =>   { 
			var urlUserDownloadTask = DownloadURLTask(  
										specName = "Download for " + userURI, 
										url= userURI + "&all_elements=yes", 
										saveToFile = true,
										nextTask = XmlParseTask(  
														specName = "Parse XML",
														nextTask = CompositeTasks( specName="composite process user",
																				tasks = List(
																							ProcessUserNode ( 
																								specName = "Process user node " ,
																								//nextTask = PrintInput()
																								nextTask = AccumulateNodes("nodes")
																							),
																							ProcessUserEdges ( 
																								specName = "Process user edges" ,
																								//nextTask = PrintInput()
																								nextTask = AccumulateEdges("edges")
																							)
																						)
																	)
													).waitForNext()
										).waitForNext()
		                    
		    waitFor(urlUserDownloadTask)
		    //usersFetchTasks = urlUserDownloadTask :: usersFetchTasks
		 	taskRunner  !  urlUserDownloadTask;


 		} )


	}

}
object NodeCounters {
	var _nodes:Int = 0

	def incrementNodes() = { 
		this.synchronized 
		{
			_nodes = _nodes + 1;
			println("NODE: " + _nodes)
		}
	}
}

case class ProcessUserNode ( override val specName: String 
	,  override val nextTask: TaskBase = null
) extends Task[Elem, Elem ]  {
	
	override def act( replyTo: akka.actor.ActorRef) = { 
		var name:String = input \ "name" text
		var id:String = input \ "id" text
		var uri:String = input.attribute("uri").get.toString
		var avatar:String = (input \ "avatar")(0) \ "@resource" text
		var alltext:String = ""
		def concatContent (x:scala.xml.Node):Boolean = { 
			if( x.label != "#PCDATA") {
				alltext += " _" + x.label ; //+ " " + x.text; 
			}
			else {
				alltext += " " + x.text; 	
			}
			processContent(x) 
			return true
		}
			
		def processContent(e:scala.xml.Node) = { List ( e child ).flatten foreach( (x)=> concatContent(x)  ) }

		processContent(input)
		NodeCounters.incrementNodes
		
		var node = 
				<node id={uri}>
					<data key="uri">{uri}</data>
					<data key="type">myexperiments.org/user</data>
					<data key="resourcePictureURL">{avatar}</data>
					<data key="name">{name}</data>
					<data key="content">{alltext}</data>
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


