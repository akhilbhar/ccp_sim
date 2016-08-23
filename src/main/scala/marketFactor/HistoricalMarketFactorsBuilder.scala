package marketFactor

import java.time.LocalDate

import data.DataFetcher
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.OneDayForecastMarketFactorsGenerator.CurrentFactors
import model.{Equity, Portfolio}
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

import scalaz.concurrent.Task

/**
  * Created by dennis on 21/8/16.
  */
case class HistoricalMarketFactorsBuilder(dataFetcher: DataFetcher) extends MarketFactorsBuilder {
  override def oneDayForecastMarketFactors(
      portfolio: Portfolio,
      date: LocalDate,
      parameters: MarketFactorsParameters): OneDayMarketForecastFactorsGenerator = {
    val equities: List[Equity] = portfolio.positions
      .map(_.instrument)
      .map {
        case e: Equity => e
      }
      .sortBy(_.ticker)

    val currentFactors: Map[Equity, CurrentFactors] = equities.map(equity => {
//      def error(err: DataError) = sys.error(s"Data for $equity not available. Error: $err")

      val price = dataFetcher.historicalPrice(equity, date) //dataActor ! HistoricalPrice(equity, date)

      lazy val priceHistory =
        dataFetcher.historicalPrices(equity, date.minusDays(parameters.horizon), date)

      val volatility = Task(
        (new StandardDeviation).evaluate(priceHistory.unsafePerformSync.map(_.adjusted).toArray))

      Task({
        val prices = priceHistory.unsafePerformSync
        prices.foldLeft(0)((acc, p) => acc + p) / prices.length
      })

      equity -> CurrentFactors(price, priceHistory, volatility)
    })(scala.collection.breakOut)
  }
}
