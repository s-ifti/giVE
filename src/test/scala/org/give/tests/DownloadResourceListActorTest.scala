package org.give.tests

import org.scalatest.FunSpec
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers





import org.give.imports.actors._
import org.give.imports.messages._

class IDownloadResourceListActorTest  extends FunSpec with ShouldMatchers  {
  

   def fixture = new {

	import akka.actor.actorRef2Scala
	import akka.actor.ActorSystem
	import akka.actor.Props
	import akka.pattern.ask
	import akka.util.Timeout

   	val _system = ActorSystem("giVE")
      val url = ImportURLSpec(  specName = "Test", url="http://www.myexperiment.org/user.xml?id=23" )

 	val tasksTracker = _system.actorOf( Props[TasksTracker], name= "tasksTracker" )
   }

   describe("Specs for DownloadResourceListActor" ) {

         it ("can send message") {
            TasksTracker.requests should be (Nil) 

             fixture.tasksTracker  !  fixture.url
         }
         Thread.sleep(5000)
         it ("can rx message") {
            TasksTracker.requests.head.state should  be ( _ : Requested )
          }

         it("can process message and return success") {
              println("processing .... ")

                var k:Int = 0;
                do { 
                    Thread.sleep(1000);
                    if( !TasksTracker.results.isEmpty   ) {
                      k = 20;
                    }
                    k = k+1
                    println("waiting .... "+ k)
                } while( k <  20  );

            TasksTracker.results should not be (List())
            val res0 = TasksTracker.results.head

            res0.state should  be ( _ : Processed )
            res0.success should  be (true)
            res0.message should be (  "David De Roure 2.0" )
            
         }
     }
  
    Thread.sleep(5000)
    println("done.... ")


    fixture._system.shutdown

}

