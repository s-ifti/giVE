package org.give.imports.messages
import java.util.Date

abstract class ActorMessageState( val whenStarted: Date  = new Date, var whenProcessed:Date = null)
case class Requested extends ActorMessageState 
case class Processed extends ActorMessageState 
case class Failed extends ActorMessageState 
 
abstract class ActorSpec( var state: ActorMessageState = Requested(), var success:Boolean = false , var message: String ="" )

case class ImportURLSpec ( val specName: String , val url: String ) extends ActorSpec 
