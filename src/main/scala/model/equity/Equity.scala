package model.equity

import java.util.Calendar

import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.{Instrument, PriceEntry}
import yahoofinance.histquotes.Interval
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.FutureInstances

/**
  * Created by dennis on 3/9/16.
  */
final case class Equity(ticker: String) extends Instrument with FutureInstances {
  override def price(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]] = {
    markToMarket
  }

  override def historicalPrice(date: Calendar): Future[Option[PriceEntry]] = {
    val from = date.clone.asInstanceOf[Calendar]
    val to = date.clone.asInstanceOf[Calendar]
    from.add(Calendar.DAY_OF_MONTH, -1)

    OptionT(historicalPrices(from, to)).map(_.headOption).run.map(_.flatten)
  }

  override def historicalPrices(from: Calendar, to: Calendar): Future[Option[Vector[PriceEntry]]] = {
    Future {
      Option(YahooFinance.get(ticker, from, to, Interval.DAILY)).map(stockToHistoricalPricesVector)
    }
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
