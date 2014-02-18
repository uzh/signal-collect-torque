import sbt._
import Keys._

object GraphsBuild extends Build {
  lazy val scCore = ProjectRef(file("../signal-collect"), id = "signal-collect")
  val scTorque = Project(id = "signal-collect-torque",
    base = file(".")) dependsOn (scCore)
}
