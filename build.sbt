import sbt.Keys._

name := "ccp_sim_scala"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  // other dependencies here
  "org.scalanlp" %% "breeze" % "0.12",
//  // native libraries are not included by default. add this if you want them (as of 0.7)
//  // native libraries greatly improve performance, but increase jar sizes.
//  // It also packages various blas implementations, which have licenses that may or may not
//  // be compatible with the Apache License. No GPL code, as best I know.
  "org.scalanlp" %% "breeze-natives" % "0.12",
//  // the visualization library is distributed separately as well.
//  // It depends on LGPL code.
  "org.scalanlp" %% "breeze-viz" % "0.12"
)

//resolvers ++= Seq(
//  // other resolvers here
//  // if you want to use snapshot builds (currently 0.12-SNAPSHOT), use this.
//  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
//  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
//)

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.1"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.5",
  "org.scalaz" %% "scalaz-concurrent" % "7.2.5",
  "org.scalaz" %% "scalaz-effect" % "7.2.5",
  "org.scalaz.stream" %% "scalaz-stream" % "0.8",
  "com.assembla.scala-incubator" %% "graph-core" % "1.11.0",
  "com.softwaremill.macwire" %% "macros" % "2.2.3" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.2.3",
  "com.typesafe.akka" %% "akka-stream" % "2.4.9"
)