giVe	=>	g=Ve

Create default tools/frameworks for processing/importing public datasets into Graph DBs.
Target is to build import process using GraphML for db independence.
Initial implementation to target Neo4J.
Approach is similar to Cascading and Tinkerpop pipes but geared towards simpler web centric imports/extracts.
 Beware this is alpha code.

Activity
07/13/2013 - Implement generalized flow framework similar to PIPE abstraction on top of akka actor
				This will allow generalized construction of various tasks that are to be performed when crawling and extracting public datasets for import to neo4j

08/10/2013 - Support dependency between tasks using observer pattern

An example use of flow
<code>

	//download all users and their detailed XMLs


 	getUsersTask = DownloadURLTask( 


 						specName = "Download", url="http://www.myexperiment.org/users.xml?num=25", page = 1 , 


						nextTask = XmlParseTask( 


										specName = "Parse XML",


										nextTask = IterateUsersTask( 


														specName="Iterate Users", 


														taskRunner = tasks,


														nextTask = LoopbackTask ( backToTask  = { ()=> getUsersTask } , 


																loopUntil = {  (mySelf)  =>   


																	!mySelf.input.asInstanceOf[ Seq[String] ].isEmpty  


																} )


													)

									)
					
					)

	tasksMover ! getUsersTask 


</code>