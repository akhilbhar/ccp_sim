package data

import java.time.LocalDate

import model.{Equity, Price}

import scalaz.concurrent.Task

trait DataFetcher extends { // Actor {
//  sealed trait DataError
//  case class DataUnavailable(error: Throwable) extends DataError

//  override def receive: Receive = {
//    case HistoricalPrice(equity, date) => historicalPrice(equity, date)
//    case HistoricalPrices(equity, from, to) => historicalPrices(equity, from, to)
//  }

  def historicalPrice(equity: Equity, date: LocalDate): Task[Option[Price]]
  def historicalPrices(equity: Equity, from: LocalDate, to: LocalDate): Task[Vector[Price]]
}
