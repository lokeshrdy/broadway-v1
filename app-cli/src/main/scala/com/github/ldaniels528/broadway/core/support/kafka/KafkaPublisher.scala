package com.github.ldaniels528.broadway.core.support.kafka

import java.util.Properties

import com.github.ldaniels528.commons.helpers.PropertiesHelper._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

/**
  * Kafka Publisher
  * @author lawrence.daniels@gmail.com
  */
class KafkaPublisher(config: Properties) {
  private var producer: Option[KafkaProducer[Array[Byte], Array[Byte]]] = None

  /**
    * Opens the connection to the message publisher
    */
  def open() {
    producer = Option(new KafkaProducer(config))
  }

  /**
    * Shuts down the connection to the message publisher
    */
  def close() {
    producer.foreach(_.close)
    producer = None
  }

  /**
    * Transports a message to the messaging server
    * @param topic   the given topic name (e.g. "greetings")
    * @param key     the given message key
    * @param message the given message payload
    */
  def publish(topic: String, key: Array[Byte], message: Array[Byte]) = {
    producer match {
      case Some(kp) => kp.send(new ProducerRecord(topic, key, message))
      case None =>
        throw new IllegalStateException("No connection established. Use open() to connect.")
    }
  }

  /**
    * Transports a message to the messaging server
    * @param rec the given [[ProducerRecord producer record]]
    */
  def publish(rec: ProducerRecord[Array[Byte], Array[Byte]]) = {
    producer match {
      case Some(kp) => kp.send(rec)
      case None =>
        throw new IllegalStateException("No connection established. Use open() to connect.")
    }
  }

}

/**
  * Verify Kafka Publisher
  * @author lawrence.daniels@gmail.com
  */
object KafkaPublisher {

  def apply(brokers: Seq[Broker]): KafkaPublisher = {
    val m = Map[String, Object](
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> mkBrokerList(brokers),
      ProducerConfig.RETRIES_CONFIG -> "3",
      ProducerConfig.ACKS_CONFIG -> "all",
      ProducerConfig.COMPRESSION_TYPE_CONFIG -> "none",
      ProducerConfig.BATCH_SIZE_CONFIG -> (200: Integer),
      ProducerConfig.MAX_BLOCK_MS_CONFIG -> (15000: Integer),
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG -> "org.apache.kafka.common.serialization.ByteArraySerializer",
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG -> "org.apache.kafka.common.serialization.ByteArraySerializer")
    new KafkaPublisher(m.toProps)
  }

  def apply(zk: ZkProxy): KafkaPublisher = {
    val brokers = KafkaMicroConsumer.getBrokerList(zk) map (b => Broker(b.host, b.port))
    KafkaPublisher(brokers)
  }

  private def mkBrokerList(brokers: Seq[Broker]): String = {
    brokers map (b => s"${b.host}:${b.port}") mkString ","
  }

}