package org.give.tests

import org.scalatest.FunSpec
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers





import org.give.flow.mover.TasksMover
import org.give.flow.tasks._
import org.give.flow.tasks.util._


class ImportURLSpecTest  extends FunSpec with ShouldMatchers  {
   describe("Specs for ImportURLSpec" ) {
       it ("can instantiate ImportURLSpec ") {
           val importURL =  DownloadURLTask(  specName = "Test", url="http://fake.com" )
           importURL.specName should be ("Test")
       }
   }



}

