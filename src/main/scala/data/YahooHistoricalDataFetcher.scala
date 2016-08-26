package data

import java.time.{Instant, LocalDate, ZoneId}
import java.util.Calendar

import model.{Equity, Price}
import yahoofinance.histquotes.Interval
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.FutureInstances

// ??
case object YahooHistoricalDataFetcher extends DataFetcher with FutureInstances {
  override def historicalPrice(equity: Equity, date: LocalDate): Future[Option[Price]] = {
    OptionT(historicalPrices(equity, date, date)).map(_.head).run
  }

  override def historicalPrices(equity: Equity,
                                from: LocalDate,
                                to: LocalDate): Future[Option[Vector[Price]]] = {
    Future {
      val convertedFrom = localDateToCalendar(from.minusDays(1))
      val convertedTo = localDateToCalendar(to.plusDays(1))

      Option(YahooFinance.get(equity.ticker, convertedFrom, convertedTo, Interval.WEEKLY))
        .map(stockToHistoricalPricesVector)
    }
  }

  private def localDateToCalendar(date: LocalDate): Calendar = {
    val calendar = Calendar.getInstance()
    calendar.setTimeInMillis(date.atStartOfDay(ZoneId.systemDefault()).toInstant.toEpochMilli)

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
