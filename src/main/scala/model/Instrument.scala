package model

import java.util.Calendar

import spire.math.Real

sealed trait Instrument

case class Equity(ticker: String) extends Instrument

sealed trait EquityOption extends Instrument {
  def underlying: Equity
}

case class PutOption(underlying: Equity, strike: Real, maturity: Calendar) extends EquityOption
case class CallOption(underlying: Equity, strike: Real, maturity: Calendar) extends EquityOption
