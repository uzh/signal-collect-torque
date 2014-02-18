/*
 *  @author Philip Stutz
 *
 *  Copyright 2014 University of Zurich
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

package com.signalcollect.deployment

import com.signalcollect.util.RandomString
import com.typesafe.config.Config
import com.signalcollect.nodeprovisioning.torque.TorqueJobSubmitter
import com.signalcollect.nodeprovisioning.torque.TorqueHost
import com.signalcollect.nodeprovisioning.torque.Job
import scala.collection.JavaConversions._

object TorqueDeployer extends App {

  def deploy(config: Config) {
    val serverAddress = config.getString("deployment.torque.server.address")
    val serverUsername = config.getString("deployment.torque.server.username")
    val jobNumberOfNodes = config.getInt("deployment.torque.job.number-of-nodes")
    val jobCoresPerNode = config.getInt("deployment.torque.job.cores-per-node")
    val jobMemory = config.getString("deployment.torque.job.memory")
    val jobWalltime = config.getString("deployment.torque.job.walltime")
    val jobWorkingDir = config.getString("deployment.torque.job.working-dir")
    val deploymentJar = config.getString("deployment.jvm.deployed-jar")
    val deploymentJvmPath = config.getString("deployment.jvm.binary-path")
    val deploymentJvmParameters = config.getString("deployment.jvm.parameters")
    val jobSubmitter = new TorqueJobSubmitter(username = serverUsername, hostname = serverAddress)
    if (config.hasPath("deployment.setup.copy-files")) {
      val copyConfigs = config.getConfigList("deployment.setup.copy-files")
      for (copyConfig <- copyConfigs) {
        val localCopyPath = copyConfig.getString("local-path")
        val remoteCopyPath = copyConfig.getString("remote-path")
        jobSubmitter.copyFileToCluster(localCopyPath, remoteCopyPath)
      }
    }
    val deploymentAlgorithm = config.getString("deployment.algorithm.class")
    val parameterMap = config.getConfig("deployment.algorithm.parameters").entrySet.map {
      entry => (entry.getKey, entry.getValue.unwrapped.toString)
    }.toMap
    val priorityString = s"#PBS -l walltime=$jobWalltime,mem=$jobMemory"
    val akkaPort = 2552
    val torque = new TorqueHost(
      jobSubmitter = jobSubmitter,
      coresPerNode = jobCoresPerNode,
      localJarPath = deploymentJar,
      jdkBinPath = deploymentJvmPath,
      jvmParameters = deploymentJvmParameters,
      priority = priorityString,
      workingDir = jobWorkingDir)
    def jobId = s"sc-${RandomString.generate(6)}"
    torque.executeJobs(List(Job(
      execute = TorqueNodeBootstrap(deploymentAlgorithm, parameterMap, jobNumberOfNodes, akkaPort).torqueExecutable,
      jobId = jobId,
      numberOfNodes = jobNumberOfNodes)))
  }
}
