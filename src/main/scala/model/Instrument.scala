package model

sealed trait Instrument

case class Equity(ticker: String) extends Instrument

case object Cash extends Instrument