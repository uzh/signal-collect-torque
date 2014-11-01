import AssemblyKeys._ 
assemblySettings

/** Project */
name := "signal-collect-torque"

version := "1.0-SNAPSHOT"

organization := "com.signalcollect"

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-optimize", "-Yinline-warnings", "-feature", "-deprecation", "-Xelide-below", "INFO" )

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true

test in assembly := {}

parallelExecution in Test := false

/** Dependencies */
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile",
  "ch.ethz.ganymed" % "ganymed-ssh2" % "build210"  % "compile",
  "commons-codec" % "commons-codec" % "1.7"  % "compile"
  )
