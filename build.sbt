import AndroidKeys._

libraryDependencies += "org.scalaz" % "scalaz-core_2.9.1" % "6.0.3"

libraryDependencies += "com.jcraft" % "jsch" % "0.1.48"

//libraryDependencies += "net.liftweb" % "lift-json_2.9.1" % "2.4"

//libraryDependencies += "net.liftweb" % "lift-json-scalaz_2.9.1" % "2.4"

//resolvers += "repo.codahale.com" at "http://repo.codahale.com"

//libraryDependencies += "com.codahale" % "jerkson_2.9.1" % "0.5.0"

resolvers += "Virtual-Void repository" at "http://mvn.virtual-void.net"

addCompilerPlugin("net.virtualvoid" % "scala-enhanced-strings_2.9.1" % "0.5.2")


