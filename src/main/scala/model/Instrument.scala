package model

sealed trait Instrument

case class Equity(ticker: String) extends Instrument