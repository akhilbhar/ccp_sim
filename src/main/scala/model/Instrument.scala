package model

import java.util.Calendar

sealed trait Instrument

case class Equity(ticker: String) extends Instrument

sealed trait EquityOption extends Instrument {
  def underlying: Equity
}

case class PutOption(underlying: Equity, strike: Double, maturity: Calendar) extends EquityOption
case class CallOption(underlying: Equity, strike: Double, maturity: Calendar) extends EquityOption
