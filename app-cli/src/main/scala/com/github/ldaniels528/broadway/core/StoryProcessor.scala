package com.github.ldaniels528.broadway.core

import java.io.File

import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Story Processor
  *
  * @author lawrence.daniels@gmail.com
  */
class StoryProcessor() {
  private val logger = LoggerFactory.getLogger(getClass)

  def load(configFile: File): Option[StoryConfig] = {
    logger.info(s"Loading ETL config '${configFile.getAbsolutePath}'...")
    StoryConfigParser(configFile).parse
  }

  def run(configFile: File)(implicit ec: ExecutionContext) {
    load(configFile) match {
      case Some(config) => run(config)
      case None =>
        throw new IllegalArgumentException(s"ETL configuration file '${configFile.getName}' is invalid")
    }
  }

  /**
    * Executes the ETL processing
    *
    * @param config the given [[StoryConfig ETL configuration]]
    */
  def run(config: StoryConfig)(implicit ec: ExecutionContext) {
    logger.info(s"Executing story '${config.id}'...")
    config.triggers foreach (_.execute(config))

    Thread.sleep(1.seconds.toMillis)
    logger.info("*" * 30 + " PROCESS COMPLETED " + "*" * 30)
  }

}