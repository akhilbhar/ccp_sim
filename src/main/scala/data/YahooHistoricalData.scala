package data

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Calendar

import model.{Equity, Price}
import module.DataActor
import yahoofinance.histquotes.Interval
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, \/, \/-}

case class YahooDataActor(name: String) extends DataActor {
  override def historicalPrice(equity: Equity, date: LocalDate): DataError \/ Option[Price] = {
    historicalPrices(equity, date, date).map(_.headOption)
  }

  override def historicalPrices(equity: Equity,
                                from: LocalDate,
                                to: LocalDate): DataError \/ Vector[Price] = {

    val convertedFrom = localDateToCalendar(from)
    val convertedTo = localDateToCalendar(to)

    Try(YahooFinance.get(equity.ticker, convertedFrom, convertedTo, Interval.DAILY)) match {
      case Success(stock) => \/-(stockToHistoricalPricesVector(stock))
      case Failure(e) => -\/(DataUnavailable(e))
    }
  }

  private def localDateToCalendar(date: LocalDate): Calendar = {
    val calendar = Calendar.getInstance()
    calendar.setTimeInMillis(date.toEpochDay)

    calendar
  }

  private def calendarToLocalDate(date: Calendar): LocalDate = {
    Instant.ofEpochMilli(date.getTimeInMillis).atZone(ZoneId.systemDefault()).toLocalDate
  }

  private def stockToHistoricalPricesVector(stock: Stock): Vector[Price] = {
    JavaConversions
      .asScalaBuffer(stock.getHistory)
      .map(
        quote =>
          Price(calendarToLocalDate(quote.getDate),
                quote.getOpen,
                quote.getHigh,
                quote.getLow,
                quote.getClose,
                quote.getVolume,
                quote.getAdjClose))
      .toVector
  }
}
