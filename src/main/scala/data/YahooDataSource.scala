package data

import java.util.Calendar

import instrument.Equity
import model.PriceEntry
import yahoofinance.histquotes.Interval
import yahoofinance.quotes.stock.StockQuote
import yahoofinance.{Stock, YahooFinance}

import scala.collection.JavaConversions
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.std.FutureInstances
import scalaz.{-\/, EitherT, \/, \/-}

/**
  * Created by dennis on 8/9/16.
  */
object YahooDataSource extends DataSource[Equity] with FutureInstances {
  private val memoizedPrices = new TrieMap[(Equity, Calendar, Calendar), Future[DataError \/ Vector[PriceEntry]]]()
  private val memoizedDividends = new TrieMap[Equity, Future[DataError \/ Double]]

  override def historicalPrice(equity: Equity, date: Calendar): Future[DataError \/ PriceEntry] = {
    val from = date.clone.asInstanceOf[Calendar]
    val to = date.clone.asInstanceOf[Calendar]
    from.add(Calendar.DAY_OF_MONTH, -1)

    if (isToday(date)) {
      Future {
        Try(YahooFinance.get(equity.ticker, from, to, Interval.DAILY)) match {
          case Success(v) =>
            Option(v) match {
              case Some(stock) => \/-(stockQuoteToPriceEntry(stock.getQuote, date))
              case None => -\/(DataNotFound(equity))
            }
          case Failure(e) => -\/(DataFetchingError(e))
        }
      }
    } else {
      EitherT(historicalPrices(equity, from, to)).map(_.head).run
    }
  }

  override def historicalPrices(equity: Equity,
                                from: Calendar,
                                to: Calendar): Future[DataError \/ Vector[PriceEntry]] = {
    memoizedPrices.getOrElseUpdate((equity, from, to), Future {
      Try(YahooFinance.get(equity.ticker, from, to, Interval.DAILY)) match {
        case Success(v) =>
          Option(v) match {
            case Some(stock) => \/-(stockToHistoricalPricesVector(stock))
            case None => -\/(DataNotFound(equity))
          }
        case Failure(e) => -\/(DataFetchingError(e))
      }
    })
  }

  override def dividendYield(equity: Equity): Future[DataError \/ Double] =
    memoizedDividends.getOrElseUpdate(equity, Future {
      Try(YahooFinance.get(equity.ticker).getDividend.getAnnualYield.doubleValue) match {
        case Success(dividend) =>
          Option(dividend) match {
            case Some(v) => \/-(v / 100.0)
            case None => -\/(DividendNotFound(equity))
          }
        case Failure(e) => -\/(DataFetchingError(e))
      }
    })

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

  private def stockQuoteToPriceEntry(quote: StockQuote, date: Calendar) = {
    PriceEntry(date,
               0, //quote.getOpen.doubleValue(), // TODO Option type!
               0, // quote.getDayHigh.doubleValue(),
               0, // quote.getDayLow.doubleValue(),
               quote.getAsk.doubleValue(),
               quote.getAvgVolume,
               quote.getAsk.doubleValue())
  }

  private def isToday(date: Calendar) = {
    val now = Calendar.getInstance

    now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
    now.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
    now.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)
  }
}
