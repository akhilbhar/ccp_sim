package data

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Calendar

import model.{Equity, Price}
import yahoofinance.histquotes.Interval
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.util.{Failure, Success, Try}
import scalaz.concurrent.Task

class YahooHistoricalDataFetcher extends DataFetcher {
  override def historicalPrice(equity: Equity, date: LocalDate): Task[Option[Price]] = {
    historicalPrices(equity, date, date).map(_.headOption)
  }

  override def historicalPrices(equity: Equity,
                                from: LocalDate,
                                to: LocalDate): Task[Vector[Price]] = {
    //Task({
      val convertedFrom = localDateToCalendar(from)
      val convertedTo = localDateToCalendar(to)

      Try(YahooFinance.get(equity.ticker, convertedFrom, convertedTo, Interval.DAILY)) match {
        case Success(stock) => stockToHistoricalPricesVector(stock)
        case Failure(e) => throw new Exception("Data unavailable for $equity.")
      }
    //})
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
                quote.getOpen.doubleValue(),
                quote.getHigh.doubleValue(),
                quote.getLow.doubleValue(),
                quote.getClose.doubleValue(),
                quote.getVolume,
                quote.getAdjClose.doubleValue()))
      .toVector
  }
}
