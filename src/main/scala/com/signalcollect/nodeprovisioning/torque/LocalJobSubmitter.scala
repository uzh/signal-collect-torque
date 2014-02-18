/*
 *  @author Daniel Strebel
 *
 *  Copyright 2013 University of Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.signalcollect.nodeprovisioning.torque

import java.io.File
import scala.sys.process._
import language.postfixOps

class LocalJobSubmitter extends AbstractJobSubmitter {

  override def runOnClusterNodes(
    jobId: String,
    numberOfNodes: Int,
    coresPerNode: Int,
    jarname: String,
    mainClass: String,
    priority: String = TorquePriority.superfast,
    jvmParameters: String,
    jdkBinPath: String = "",
    workingDir: String = "/home/torque/tmp/${USER}.${PBS_JOBID}",
    mailAddress: Option[String] = None): String = {
    val script = getShellScript(
      jobId,
      numberOfNodes,
      coresPerNode,
      jarname,
      mainClass,
      priority,
      jvmParameters,
      jdkBinPath,
      workingDir,
      mailAddress)
    val qsubCommand = """echo """ + script + """ | qsub"""
    Seq("echo", script) #| Seq("qsub")!!
  }

  def executeCommandOnClusterManager(command: String): String = {
    println(command)
    command!!
  }

  def copyFileToCluster(localPath: String, targetDir: String = System.getProperty("user.home")) {
    val localParentDir = new File(localPath)

    if (localParentDir.getAbsolutePath() != targetDir) {
      try {
        Seq("cp", localPath, targetDir).!!
      } catch {
        case _: Throwable =>
      }

    }

  }
}