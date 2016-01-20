package com.github.ldaniels528.broadway.cli.io

/**
  * Unsupported Data Type Exception
  */
class UnsupportedDataTypeException(data: Data)
  extends RuntimeException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")