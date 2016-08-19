package data

import java.io.IOException
import java.time.{Instant, LocalDate, ZoneId}
import java.util.Calendar

import model.{Equity, HistoricalPrice}
import module.DataModule
import yahoofinance.histquotes.Interval
import yahoofinance.YahooFinance

import scala.collection.JavaConversions
import scalaz.{-\/, \/, \/-}

/**
  * Created by dennis on 19/8/16.
  */
trait HistoricalData extends DataModule {
  protected object Data extends Data {
    override def historicalPrices(
        equity: Equity,
        from: LocalDate,
        to: LocalDate): DataError \/ Vector[HistoricalPrice] = {
      try {
        val convertedFrom = localDateToCalendar(from)
        val convertedTo = localDateToCalendar(to)

        val stock = YahooFinance
          .get(equity.ticker, convertedFrom, convertedTo, Interval.DAILY)

        val historicalPrices = JavaConversions
          .asScalaBuffer(stock.getHistory)
          .map(
            quote =>
              HistoricalPrice(calendartoLocalDate(quote.getDate),
                              quote.getOpen,
                              quote.getHigh,
                              quote.getLow,
                              quote.getClose,
                              quote.getVolume,
                              quote.getAdjClose))
          .toVector

        \/-(historicalPrices)
      } catch {
        case e: IOException => -\/(DataUnavailable(e))
      }
    }

    override def historicalPrice(
        equity: Equity,
        date: LocalDate): DataError \/ Option[HistoricalPrice] = {
      historicalPrices(equity, date, date).map(_.headOption)
    }

    private def localDateToCalendar(date: LocalDate): Calendar = {
      val calendar = Calendar.getInstance()
      calendar.setTimeInMillis(date.toEpochDay)

      calendar
    }

    private def calendartoLocalDate(date: Calendar): LocalDate = {
      Instant
        .ofEpochMilli(date.getTimeInMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate
    }
  }
}
