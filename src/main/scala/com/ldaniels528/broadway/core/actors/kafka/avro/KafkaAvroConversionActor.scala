package com.ldaniels528.broadway.core.actors.kafka.avro

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.Actors._
import com.ldaniels528.broadway.core.actors.kafka.avro.KafkaAvroConversionActor._
import com.ldaniels528.trifecta.io.avro.AvroConversion
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory

/**
 * Kafka-Avro Conversion Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaAvroConversionActor(schemaString: String) extends Actor {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  @transient private lazy val schema = new Schema.Parser().parse(schemaString)

  override def receive = {
    case Decode(bytes, target) =>
      target ! AvroConversion.decodeRecord(schema, bytes)

    case Encode(record, target) =>
      target ! AvroConversion.encodeRecord(record)

    case message =>
      logger.warn(s"Unhandled message $message")
      unhandled(message)
  }
}

/**
 * Kafka-Avro Decoding Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object KafkaAvroConversionActor {

  case class Decode(bytes: Array[Byte], target: BWxActorRef)

  case class Encode(record: GenericRecord, target: BWxActorRef)

}
