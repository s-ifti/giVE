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

 	val tasks = _system.actorOf( Props[TasksMover], name= "tasksMover" );


	//download all users and their detailed XMLs
 	val getUsers = DownloadURLTask( specName = "Download", url="http://www.myexperiment.org/users.xml", page = 1 ) 
	getUsers.nextSpec = XmlParseTask( specName = "Parse XML") 
	getUsers.nextSpec.nextSpec = IterateUsersTask( specName="Iterate Users", tasks)
	getUsers.nextSpec.nextSpec.nextSpec  = LoopbackTask ( task  = getUsers , 
													loopUntil = {  (mySelf)  =>   
														!mySelf.input.asInstanceOf[ Seq[String] ].isEmpty  
													} )
	tasks ! getUsers

	//Thread.sleep(25000)	
	readLine
	//TasksMover.results map println

	_system.shutdown
}

