package data

import java.util.Calendar

import model.{Equity, Price}
import yahoofinance.histquotes.Interval
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.FutureInstances

/**
  * Fetches data from Yahoo Finance
  */
case object YahooHistoricalDataFetcher extends DataFetcher with FutureInstances {
  override def historicalPrice(equity: Equity, date: Calendar): Future[Option[Price]] = {
    val from = date.clone.asInstanceOf[Calendar]
    val to = date.clone().asInstanceOf[Calendar]
    from.add(Calendar.DAY_OF_MONTH, -1)

    OptionT(historicalPrices(equity, from, to)).map(_.headOption).run.map(_.flatten)
  }

  override def historicalPrices(equity: Equity,
                                from: Calendar,
                                to: Calendar): Future[Option[Vector[Price]]] = {
    Future {
      Option(YahooFinance.get(equity.ticker, from, to, Interval.DAILY))
        .map(stockToHistoricalPricesVector).map(_.map(println(_)))

      Option(YahooFinance.get(equity.ticker, from, to, Interval.DAILY))
        .map(stockToHistoricalPricesVector)
    }
  }

  private def stockToHistoricalPricesVector(stock: Stock): Vector[Price] = {
    JavaConversions
      .asScalaBuffer(stock.getHistory)
      .map(
        quote =>
          Price(quote.getDate,
                quote.getOpen.doubleValue(),
                quote.getHigh.doubleValue(),
                quote.getLow.doubleValue(),
                quote.getClose.doubleValue(),
                quote.getVolume,
                quote.getAdjClose.doubleValue())
      )
      .toVector
  }
}
