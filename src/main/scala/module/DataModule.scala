package module

import java.time.LocalDate

import model.{Equity, HistoricalPrice}

import scalaz.\/

/**
  * Created by dennis on 18/8/16.
  */
trait DataModule {
  sealed trait DataError
  case class DataUnavailable(error: Throwable) extends DataError

  protected def data: Data

  trait Data {
    def historicalPrices(equity: Equity, from: LocalDate, to: LocalDate): DataError \/ Vector[HistoricalPrice]
    def historicalPrice(equity: Equity, date: LocalDate): DataError \/ Option[HistoricalPrice]
  }
}
