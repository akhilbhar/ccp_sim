package data

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Calendar

import model.{Equity, Price}
import yahoofinance.histquotes.Interval
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.concurrent.Future
import scalaz.OptionT

class YahooHistoricalDataFetcher extends DataFetcher {
  override def historicalPrice(equity: Equity, date: LocalDate): Future[Option[Price]] = {
    OptionT(historicalPrices(equity, date, date)).map(_.head).run
  }

  override def historicalPrices(equity: Equity,
                                from: LocalDate,
                                to: LocalDate): Future[Option[Vector[Price]]] = {
    Future {
      val convertedFrom = localDateToCalendar(from)
      val convertedTo = localDateToCalendar(to)

      Option(YahooFinance.get(equity.ticker, convertedFrom, convertedTo, Interval.DAILY))
        .map(stockToHistoricalPricesVector)
    }
  }

  private def localDateToCalendar(date: LocalDate): Calendar = {
    val calendar = Calendar.getInstance()
    calendar.setTimeInMillis(date.toEpochDay)

    calendar
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

  private def calendarToLocalDate(date: Calendar): LocalDate = {
    Instant.ofEpochMilli(date.getTimeInMillis).atZone(ZoneId.systemDefault()).toLocalDate
  }
}
