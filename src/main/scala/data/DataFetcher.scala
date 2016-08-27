package data

import java.util.Calendar

import model.{Equity, Price}

import scala.concurrent.Future

trait DataFetcher {
  def historicalPrice(equity: Equity, date: Calendar): Future[Option[Price]]

  def historicalPrices(equity: Equity,
                       from: Calendar,
                       to: Calendar): Future[Option[Vector[Price]]]
}
