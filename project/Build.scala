import sbt._
import Keys._

object GraphsBuild extends Build {
  lazy val scCore = ProjectRef(file("../signal-collect"), id = "signal-collect")
  val scDeployment = Project(id = "signal-collect-deployment",
    base = file(".")) dependsOn (scCore)
}
