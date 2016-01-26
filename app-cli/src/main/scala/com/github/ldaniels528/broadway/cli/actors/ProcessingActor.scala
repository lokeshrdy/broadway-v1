package com.github.ldaniels528.broadway.cli.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool

/**
  * Processing Actor
  */
class ProcessingActor() extends Actor with ActorLogging {

  override def receive = {
    case task: Runnable =>
      task.run()

    case message =>
      log.warning(s"Unhandled message '$message' (${Option(message).map(_.getClass.getName).orNull})")
      unhandled(message)
  }

}

/**
  * Processing Actor Companion Object
  */
object ProcessingActor {
  private val actors = TaskActorSystem.system.actorOf(Props[ProcessingActor].withRouter(RoundRobinPool(nrOfInstances = 1)))

  def !(task: Runnable) = actors ! task

}