package model

import model.Factor.{Price, Volatility}

/**
  * Created by dennis on 15/8/16.
  */
sealed trait Factor

object Factor {
  case class Price(equity: Equity) extends Factor
  case class Volatility(equity: Equity) extends Factor
}

trait Factors {
  def apply(factor: Factor): Option[Double] = factor match {
    case Price(equity) => price(equity)
    case Volatility(equity) => volatility(equity)
  }

  protected def price(equity: Equity): Option[Double]
  protected def volatility(equity: Equity): Option[Double]
}
