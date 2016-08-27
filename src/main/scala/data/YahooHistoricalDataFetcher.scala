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

// ??
case object YahooHistoricalDataFetcher extends DataFetcher with FutureInstances {
  override def historicalPrice(equity: Equity, date: Calendar): Future[Option[Price]] = {
    val from = date
    val to = date.clone().asInstanceOf[Calendar]
    to.add(Calendar.DATE, 1)

     OptionT(historicalPrices(equity, from, to)).map(_.head).run
  }

  override def historicalPrices(equity: Equity,
                                from: Calendar,
                                to: Calendar): Future[Option[Vector[Price]]] = {
    val from = Calendar.getInstance()
    val to = Calendar.getInstance()
    from.add(Calendar.DATE, -1) // from 1 year ago

    Future {
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
