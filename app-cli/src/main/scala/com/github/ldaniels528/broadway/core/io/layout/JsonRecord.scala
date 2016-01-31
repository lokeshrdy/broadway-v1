package com.github.ldaniels528.broadway.core.io.layout


import com.github.ldaniels528.broadway.core.io.layout.RecordTypes._

/**
  * Json Record implementation
  */
case class JsonRecord(id: String, fields: Seq[Field], `type`: RecordType)
  extends TextRecord with JsonCapability {

  override def duplicate = this.copy()

  override def fromLine(line: String) = fromJson(line)

  override def toLine = toJson.toString()

}
