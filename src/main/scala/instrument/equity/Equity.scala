package instrument.equity

import java.util.Calendar

import instrument.Instrument
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.PriceEntry
import yahoofinance.histquotes.Interval
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.FutureInstances

/**
  * Created by dennis on 3/9/16.
  */
final case class Equity(ticker: String) extends Instrument with FutureInstances {
  private val memoizedPrices = new TrieMap[(Calendar, Calendar), Future[Option[Vector[PriceEntry]]]]()

  override def price(implicit factors: MarketFactors,
                     factorsParameters: MarketFactorsParameters): Future[Option[Double]] = {
    markToMarket
  }

  override def historicalPrice(date: Calendar): Future[Option[PriceEntry]] = {
    val from = date.clone.asInstanceOf[Calendar]
    val to = date.clone.asInstanceOf[Calendar]
    from.add(Calendar.DAY_OF_MONTH, -1)

    OptionT(historicalPrices(from, to)).map(_.headOption).run.map(_.flatten)
  }

  override def historicalPrices(from: Calendar, to: Calendar): Future[Option[Vector[PriceEntry]]] = {
    memoizedPrices.getOrElseUpdate((from, to), Future {
      Option(YahooFinance.get(ticker, from, to, Interval.DAILY)).map(stockToHistoricalPricesVector)
    })
  }

  override def dividendYield: Future[Option[Double]] = Future {
    Option(YahooFinance.get(ticker).getDividend.getAnnualYield.doubleValue / 100.0)
  }

  private def stockToHistoricalPricesVector(stock: Stock): Vector[PriceEntry] = {
    JavaConversions
      .asScalaBuffer(stock.getHistory)
      .map(
        quote =>
          PriceEntry(quote.getDate,
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
