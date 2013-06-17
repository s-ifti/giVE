package org.give.tests

import org.scalatest.FunSpec
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers





import org.give.imports.actors._
import org.give.imports.messages._

class ImportURLSpecTest  extends FunSpec with ShouldMatchers  {
   describe("Specs for ImportURLSpec" ) {
       it ("can instantiate ImportURLSpec ") {
           val importURL =  ImportURLSpec(  specName = "Test", url="http://fake.com" )
           importURL.specName should be ("Test")
       }
   }



}

