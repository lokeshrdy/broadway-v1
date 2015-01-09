package com.ldaniels528.broadway.server

import java.io.File

import akka.actor.Actor
import com.ldaniels528.broadway.BroadwayNarrative
import com.ldaniels528.broadway.core.actors.Actors._
import com.ldaniels528.broadway.core.location.{FileLocation, Location}
import com.ldaniels528.broadway.core.narrative._
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.util.FileHelper._
import com.ldaniels528.broadway.core.util.FileMonitor
import com.ldaniels528.broadway.server.BroadwayServer._
import com.ldaniels528.trifecta.util.OptionHelper._
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success}

/**
 * Broadway Server
 * @param config the given [[ServerConfig]]
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayServer(config: ServerConfig) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private implicit val system = config.system
  private implicit val ec = system.dispatcher
  private implicit val rt = new NarrativeRuntime()
  private val fileWatcher = new FileMonitor(system)
  private val reported = TrieMap[String, Throwable]()

  // create the system actors
  private val archivingActor = config.archivingActor
  private val processingActor = config.addActor(new TopologyProcessingActor(config))

  /**
   * Start the server
   */
  def start() {
    System.out.println(s"Broadway Server v$Version")

    // initialize the configuration
    config.init()

    // load the topology configurations
    val topologyConfigs = NarrativeConfig.loadNarrativeConfigs(config.getTopologiesDirectory)

    // setup listeners for all configured locations
    topologyConfigs foreach { tc =>

      // watch the "incoming" directories for processing files
      tc.locations foreach {
        case location@FileLocation(id, path, feeds) =>
          fileWatcher.listenForFiles(directory = new File(path))(handleIncomingFile(location, _))

        case location =>
          logger.warn(s"Listening is not supported by location '${location.id}'")
      }
    }

    // watch the "completed" directory for archiving files
    fileWatcher.listenForFiles(config.getCompletedDirectory)(archivingActor ! _)
    ()
  }

  /**
   * Handles the the given incoming file
   * @param file the given incoming [[File]]
   */
  private def handleIncomingFile(location: Location, file: File) {
    location.findFeed(file.getName) match {
      case Some(feed) => processFeed(feed, file)
      case None => noMappedProcess(location, file)
    }
    ()
  }

  /**
   * Processes the given feed via a topology
   * @param feed the given [[Feed]]
   * @param file the given [[File]]
   */
  private def processFeed(feed: Feed, file: File) = {
    feed.topology foreach { td =>
      // lookup the topology
      rt.getNarrative(config, td) match {
        case Success(topology) =>
          val fileName = file.getName
          logger.info(s"${topology.name}: Moving file '$fileName' to '${config.getWorkDirectory}' for processing...")
          val wipFile = new File(config.getWorkDirectory, fileName)
          move(file, wipFile)

          // start the topology using the file as its input source
          processingActor ! RunTopology(topology, FileResource(wipFile.getAbsolutePath))

        /*
        executeTopology(topology, wipFile) onComplete {
          case Success(result) =>
            move(wipFile, new File(config.getCompletedDirectory, fileName))
          case Failure(e) =>
            logger.error(s"${topology.name}: File '$fileName' failed during processing", e)
            move(wipFile, new File(config.getFailedDirectory, fileName))
        }*/
        case Failure(e) =>
          if (!reported.contains(td.id)) {
            logger.error(s"${td.id}: Topology could not be instantiated", e)
            reported += td.id -> e
          }
      }

    }
  }

  /**
   * Called when no mapping process is found for the given file
   * @param file the given [[File]]
   */
  private def noMappedProcess(location: Location, file: File) = {
    val fileName = file.getName
    logger.info(s"${location.id}: No mappings found for '$fileName'. Moving to '${config.getCompletedDirectory}' for archival.")
    move(file, new File(config.getCompletedDirectory, fileName))
  }

}

/**
 * Broadway Server Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object BroadwayServer {
  private val Version = "0.1"

  /**
   * Enables command line execution
   * {{{ broadway.sh /usr/local/java/broadway/server-config.properties }}}
   * @param args the given command line arguments
   */
  def main(args: Array[String]) {
    // load the configuration
    val config = args.toList match {
      case Nil => ServerConfig()
      case configPath :: Nil => ServerConfig(FileResource(configPath))
      case _ =>
        throw new IllegalArgumentException(s"${getClass.getName} [<config-file>]")
    }
    new BroadwayServer(config.orDie("No configuration file (broadway-config.xml) found")).start()
  }

  /**
   * This is an internal use actor that is responsible for processing topologies
   * @author Lawrence Daniels <lawrence.daniels@gmail.com>
   */
  class TopologyProcessingActor(config: ServerConfig) extends Actor {
    override def receive = {
      case RunTopology(topology, resource) =>
        topology.start(resource)
      case message =>
        unhandled(message)
    }
  }

  /**
   * This message causes the the given topology to be invoked; consuming the given resource
   * @param topology the given [[BroadwayNarrative]]
   * @param resource the given [[ReadableResource]]
   */
  case class RunTopology(topology: BroadwayNarrative, resource: ReadableResource)

}
