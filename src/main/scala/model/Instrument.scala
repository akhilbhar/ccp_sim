package model

sealed trait Instrument

/**
  * Contract between a buyer and a seller with a price.
  *
  * @param issuer the issuer of the contract.
  */
case class Equity(ticker: String) extends Instrument

case object Cash extends Instrument