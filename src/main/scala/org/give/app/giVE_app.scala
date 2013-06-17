package org.give.app

import org.give 

import akka.actor.actorRef2Scala
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout


import org.give.imports.actors._

import org.give.imports.messages._

 object giVE_app extends App { 

	println("giVE_app")

 	val _system = ActorSystem("giVE")

 	val tasks = _system.actorOf( Props[TasksTracker], name= "tasksTracker" );
 	tasks  !  ImportURLSpec(  specName = "Test", url="http://www.myexperiment.org/user.xml?id=23" )

	Thread.sleep(5000)	

	_system.shutdown
}

