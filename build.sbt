name := "giVE"

version := "0.1"

scalaVersion := "2.10.1"

scalaHome := Some(file("/Users/ifti/Downloads/scala-2.10.1"))

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test" 

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.4"


