/*
 *  @author Philip Stutz
 *  @author Thomas Keller
 *
 *  Copyright 2012 University of Zurich
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

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.signalcollect.configuration.ActorSystemRegistry
import com.signalcollect.messaging.AkkaProxy
import com.signalcollect.nodeprovisioning.NodeProvisioner
import com.typesafe.config.Config
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.japi.Creator
import akka.pattern.ask
import akka.util.Timeout
import com.signalcollect.interfaces.NodeActor
import scala.reflect.ClassTag
import com.signalcollect.interfaces.MessageBusFactory
import com.signalcollect.interfaces.GetNodes
import com.signalcollect.util.RandomString
import com.signalcollect.node.DefaultNodeActor
import com.signalcollect.util.AkkaRemoteAddress

class TorqueNodeProvisioner(
  torqueHost: TorqueHost,
  numberOfNodes: Int,
  allocateWorkersOnCoordinatorNode: Boolean,
  copyExecutable: Boolean) extends NodeProvisioner {
  def getNodes(localSystem: ActorSystem, actorNamePrefix: String, akkaConfig: Config): Array[ActorRef] = {
    val nodeProvisioner = localSystem.actorOf(Props(classOf[NodeProvisionerActor], numberOfNodes, allocateWorkersOnCoordinatorNode), name = "NodeProvisioner")
    val nodeProvisionerAddress = AkkaRemoteAddress.get(nodeProvisioner, localSystem)
    var jobs = List[Job]()
    implicit val timeout = new Timeout(Duration.create(1800, TimeUnit.SECONDS))
    val baseNodeId = {
      if (allocateWorkersOnCoordinatorNode) {
        1
      } else {
        0
      }
    }
    for (nodeId <- baseNodeId until numberOfNodes) {
      val function: () => Unit = {
        () =>
          val system = ActorSystem("SignalCollect", akkaConfig)
          val nodeController = system.actorOf(Props(classOf[DefaultNodeActor], actorNamePrefix, nodeId, numberOfNodes, Some(nodeProvisionerAddress)), name = "DefaultNodeActor" + nodeId.toString)
      }
      val jobId = s"node-$nodeId-${RandomString.generate(6)}"
      jobs = new Job(jobId = jobId, execute = function) :: jobs
    }
    torqueHost.executeJobs(jobs, copyExecutable)
    val nodesFuture = nodeProvisioner ? GetNodes
    val result = Await.result(nodesFuture, timeout.duration)
    val nodes: Array[ActorRef] = result.asInstanceOf[Array[ActorRef]]
    nodes
  }
}
