package marketFactor

import java.util.Calendar

import breeze.stats._
import data.DataFetcher
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.MarketFactorsGenerator.CurrentFactors
import model.{Equity, EquityOption, Portfolio}
import util.Math.volatilityOfChange

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.FutureInstances

/**
  * Builds market factors based on the data from the data fetcher.
  * @param dataFetcher data to be used for the market factors
  */
case class HistoricalMarketFactorsBuilder(dataFetcher: DataFetcher)
    extends MarketFactorsBuilder
    with FutureInstances {
  override def oneDayForecastMarketFactors(portfolio: Portfolio, date: Calendar)(
      implicit parameters: MarketFactorsParameters): Future[MarketFactorsGenerator] = {

    /* Equities in alphabetical order */
    val equities: List[Equity] = portfolio.positions
      .map(_.instrument)
      .map {
        case e: Equity => e
        case o: EquityOption => o.underlying
      }
      .sortBy(_.ticker)

    /* Current factors */
    val mapCurrentFactors: Map[Equity, Future[Option[CurrentFactors]]] = equities.map(equity => {
      val from = date.clone().asInstanceOf[Calendar]
      from.add(Calendar.DATE, -parameters.horizon)
      val to = date.clone().asInstanceOf[Calendar]

      /* Build current factors for equity based on data from fetcher */
      val futureCurrentFactors = (
        for {
          price <- OptionT(dataFetcher.historicalPrice(equity, date)).map(_.adjusted)
          priceHistory <- OptionT(
            dataFetcher.historicalPrices(equity, from, to).map(_.map(_.map(_.adjusted))))
        } yield CurrentFactors(price, stddev(priceHistory), priceHistory)
      ).run

      equity -> futureCurrentFactors
    })(scala.collection.breakOut)

    /* Convert map of future to future of map */
    val futureCurrentFactors = Future
      .sequence(mapCurrentFactors.map(entry => entry._2.map(i => (entry._1, i))))
      .map(_.toMap)

    for {
      currentFactors <- futureCurrentFactors
    } yield OneDayGBMMarketFactorsGenerator(date, parameters.riskFreeRate, currentFactors)
  }

  override def marketFactors(date: Calendar)(
      implicit parameters: MarketFactorsParameters): MarketFactors = {
    new MarketFactors {
      override protected def price(equity: Equity): Future[Option[Double]] = {
        OptionT(dataFetcher.historicalPrice(equity, date)).map(_.adjusted).run
      }

      override protected def volatility(equity: Equity): Future[Option[Double]] = {
        val from = date.clone().asInstanceOf[Calendar]
        from.add(Calendar.DATE, -parameters.horizon)
        val to = date.clone().asInstanceOf[Calendar]

        (for {
          priceHistory <- OptionT(
            dataFetcher.historicalPrices(equity, from, to).map(_.map(_.map(_.adjusted))))
        } yield volatilityOfChange(priceHistory)).run
      }

      override protected def daysToMaturity(maturity: Calendar): Future[Option[Double]] =
        Future.successful({
          val now = Calendar.getInstance()

          val milliseconds1: Long = now.getTimeInMillis
          val milliseconds2: Long = maturity.getTimeInMillis
          val diff: Long = milliseconds2 - milliseconds1
          val diffDays: Double = diff / (24.0 * 60.0 * 60.0 * 1000.0)

          printf(s"Diff: $diffDays")

          if (diffDays > 0) Some(diffDays) else None
        })

      override protected def riskFreeRate: Future[Option[Double]] =
        Future.successful(Some(parameters.riskFreeRate))
    }
  }
}
