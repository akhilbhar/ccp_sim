package data

import java.util.Calendar

import model.PriceEntry

import scala.concurrent.Future

trait DataSource {

  def historicalPrice(date: Calendar): Future[Option[PriceEntry]]
  def historicalPrices(from: Calendar, to: Calendar): Future[Option[Vector[PriceEntry]]]
  def dividendYield: Future[Option[Double]]

}
