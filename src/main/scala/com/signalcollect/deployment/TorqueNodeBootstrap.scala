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

import java.net.InetAddress
import com.signalcollect.configuration.AkkaConfig
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import com.signalcollect.configuration.ActorSystemRegistry
import com.signalcollect.configuration.AkkaConfig
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import com.signalcollect.interfaces.NodeActor
import akka.util.Timeout
import com.signalcollect.node.DefaultNodeActor

/**
 * A class that gets serialized and contains the code required to bootstrap
 * an actor system on a Torque cluster. The code in 'torqueExecutable()'
 * is run on each Torque cluster node. A node actor is started on all nodes,
 * and the leader additionally bootstraps a Signal/Collect computation
 * defined by the class 'torqueDeployableAlgorithmClassName'.
 */
case class TorqueNodeBootstrap[Id, Signal](
  actorNamePrefix: String,
  torqueDeployableAlgorithmClassName: String,
  parameters: Map[String, String],
  numberOfNodes: Int,
  akkaPort: Int,
  kryoRegistrations: List[String],
  kryoInitializer: String) {

  def akkaConfig(akkaPort: Int,
    kryoRegistrations: List[String],
    kryoInitializer: String) = AkkaConfig.get(
    serializeMessages = false,
    loggingLevel = Logging.InfoLevel , //Logging.DebugLevel,
    kryoRegistrations = kryoRegistrations,
    kryoInitializer = kryoInitializer,
    port = akkaPort)

  def ipAndIdToActorRef(ip: String, id: Int, system: ActorSystem, akkaPort: Int): ActorRef = {
    val address = s"""akka.tcp://SignalCollect@$ip:$akkaPort/user/DefaultNodeActor$id"""
    implicit val timeout = Timeout(30 seconds)
    val selection = system.actorSelection(address)
    val actorRef = Await.result(selection.resolveOne, 30 seconds)
    actorRef
  }

  def torqueExecutable {
    val nodesFilePath = System.getenv("PBS_NODEFILE")
    val isLeader = nodesFilePath != null
    if (isLeader) {
      println("Leader is giving other nodes some time to start ...")
      Thread.sleep(1000)
    }
    val leaderExecutionStartingTime = System.currentTimeMillis // Actual start of S/C execution.
    println(s"numberOfNodes = $numberOfNodes, akkaPort = $akkaPort")
    println(s"Starting the actor system and node actor ...")
    val nodeId = System.getenv("PBS_NODENUM").toInt
    val system: ActorSystem = ActorSystem("SignalCollect",
      akkaConfig(akkaPort, kryoRegistrations, kryoInitializer))
    ActorSystemRegistry.register(system)
    val nodeController = system.actorOf(Props(classOf[DefaultNodeActor[Id, Signal]], actorNamePrefix, nodeId, numberOfNodes, None, None), name = "DefaultNodeActor" + nodeId.toString)
    if (isLeader) {
      println("Leader is generating the node actor references ...")
      val nodeNames = io.Source.fromFile(nodesFilePath).getLines.toList.distinct
      val nodeIps = nodeNames.map(InetAddress.getByName(_).getHostAddress)
      val nodeActors = nodeIps.zipWithIndex.map { case (ip, i) => ipAndIdToActorRef(ip, i, system, akkaPort) }.toArray
      println("Leader is passing the nodes and graph builder on to the user code ...")
      val algorithmObject = Class.forName(torqueDeployableAlgorithmClassName).newInstance.asInstanceOf[TorqueDeployableAlgorithm]
      val extendedParameters = parameters + ("leaderExecutionStartingTime" -> leaderExecutionStartingTime.toString)
      algorithmObject.execute(extendedParameters, nodeActors)
    }
  }
}
