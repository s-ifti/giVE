package org.give.tests

import org.scalatest.FunSpec
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers





import org.give.flow.mover.TasksMover
import org.give.flow.tasks._
import org.give.flow.tasks.util._
import org.give.imports.tasks.myexperiments._

class IDownloadResourceListActorTest  extends FunSpec with ShouldMatchers  {
  

   def fixture = new {

    	import akka.actor.actorRef2Scala
    	import akka.actor.ActorSystem
    	import akka.actor.Props
    	import akka.pattern.ask
    	import akka.util.Timeout

     	val _system = ActorSystem("giVE")
      val urlTask = DownloadURLTask(  specName = "Download", url="http://www.myexperiment.org/user.xml?id=23" ) 
      urlTask.nextSpec = XmlParseTask( specName = "Parse XML") 
      urlTask.nextSpec.nextSpec = ParseNameTask( specName = "Parse Name" , elementName="name" )
      urlTask.nextSpec.nextSpec.nextSpec = ProcessUserSpec( specName = "Convert User to GraphML ")


     	val tasksMover = _system.actorOf( Props[TasksMover], name= "tasksMover" )
   }

   describe("Specs for DownloadResourceListActor" ) {

         it ("can send message") {
            TasksMover.requests should be (Nil) 

             fixture.tasksMover  !  fixture.urlTask
         }
         Thread.sleep(5000)
         it ("can rx message") {
            TasksMover.requests.head.getState should  be ( _ : Requested )
          }

         it("can process message and return success") {
              println("processing .... ")

                var k:Int = 0;
                do { 
                    Thread.sleep(1000);
                    if( !TasksMover.results.isEmpty   ) {
                      k = 20;
                    }
                    k = k+1
                    println("waiting .... "+ k)
                } while( k <  20  );
              Thread.sleep(3000);
            TasksMover.results should not be (List())
            val res0 = TasksMover.results.head

            res0.getState should  be ( _ : Processed )
            res0 should be (_: ProcessUserSpec )
            res0.isSuccess  should  be (true)
            res0.getOutput should be (  "ALL HAIL TO David De Roure 2.0" )
            val res1 = TasksMover.results.tail.head
            res1 should be (_: DownloadURLTask)
            res1.getState should  be ( _ : Processed )
            res1.isSuccess  should  be (true)
            val check = "David De Roure 2.0"
            res1.getOutput should be (  check  )
            
         }
     }
  
    Thread.sleep(5000)
    println("done.... ")


    fixture._system.shutdown

}

