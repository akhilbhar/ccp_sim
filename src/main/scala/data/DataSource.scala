package data

import java.util.Calendar

import instrument.Instrument
import model.PriceEntry

import scala.concurrent.Future
import scalaz.\/

trait DataSource[-I <: Instrument] {

  def historicalPrice(instrument: I, date: Calendar): Future[DataError \/ PriceEntry]
  def historicalPrices(instrument: I, from: Calendar, to: Calendar): Future[DataError \/ Vector[PriceEntry]]
  def dividendYield(instrument: I): Future[DataError \/ Double]

}

sealed trait DataError
case class DataFetchingError(throwable: Throwable) extends DataError
case class DataNotFound(instrument: Instrument) extends DataError
case class DividendNotFound(instrument: Instrument) extends DataError