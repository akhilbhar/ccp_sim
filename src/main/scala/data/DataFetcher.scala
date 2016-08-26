package data

import java.time.LocalDate

import model.{Equity, Price}

import scala.concurrent.Future

trait DataFetcher {
  def historicalPrice(equity: Equity, date: LocalDate): Future[Option[Price]]

  def historicalPrices(equity: Equity,
                       from: LocalDate,
                       to: LocalDate): Future[Option[Vector[Price]]]
}
