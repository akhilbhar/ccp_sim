package module

import java.time.LocalDate

import akka.actor.Actor
import model.{Equity, Price}
import module.DataActor.{HistoricalPrice, HistoricalPrices}

import scalaz.\/

trait DataActor extends Actor {
  sealed trait DataError
  case class DataUnavailable(error: Throwable) extends DataError

  override def receive: Receive = {
    case HistoricalPrice(equity, date) => historicalPrice(equity, date)
    case HistoricalPrices(equity, from, to) => historicalPrices(equity, from, to)
  }

  def historicalPrice(equity: Equity, date: LocalDate): DataError \/ Option[Price]
  def historicalPrices(equity: Equity, from: LocalDate, to: LocalDate): DataError \/ Vector[Price]
}

object DataActor {
  case class HistoricalPrice(equity: Equity, date: LocalDate)
  case class HistoricalPrices(equity: Equity, from: LocalDate, to: LocalDate)
}
