package marketFactor

import java.time.LocalDate

import breeze.numerics.sqrt
import data.DataFetcher
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.OneDayForecastMarketFactorsGenerator.CurrentFactors
import model.{Equity, Portfolio}

import scala.concurrent.Future
import scalaz.OptionT

/**
  * Created by dennis on 21/8/16.
  */
case class HistoricalMarketFactorsBuilder(dataFetcher: DataFetcher) extends MarketFactorsBuilder {
  override def oneDayForecastMarketFactors(portfolio: Portfolio, date: LocalDate)(
      implicit parameters: MarketFactorsParameters)
    : Future[OneDayMarketForecastFactorsGenerator] = {
    val equities: List[Equity] = portfolio.positions
      .map(_.instrument)
      .map {
        case e: Equity => e
      }
      .sortBy(_.ticker)

    val mapCurrentFactors: Map[Equity, Future[Option[CurrentFactors]]] = equities.map(equity => {
      val futureCurrentFactors = (
        for {
          price <- OptionT(dataFetcher.historicalPrice(equity, date)).map(_.adjusted)
          priceHistory <- OptionT(
            dataFetcher
              .historicalPrices(equity, date.minusDays(parameters.horizon), date)
              .map(_.map(_.map(_.adjusted))))
        } yield CurrentFactors(price, standardDeviation(priceHistory), priceHistory)
      ).run

      equity -> futureCurrentFactors
    })(scala.collection.breakOut)

    val futureCurrentFactors = Future
      .sequence(mapCurrentFactors.map(entry => entry._2.map(i => (entry._1, i))))
      .map(_.toMap)

    for {
      currentFactors <- futureCurrentFactors
    } yield OneDayMarketForecastFactorsGenerator(date, currentFactors)
  }

  override def marketFactors(date: LocalDate)(
      implicit parameters: MarketFactorsParameters): MarketFactors = {
    new MarketFactors {
      override protected def price(equity: Equity): Future[Option[Double]] =
        OptionT(dataFetcher.historicalPrice(equity, date)).map(_.adjusted).run

      override protected def volatility(equity: Equity): Future[Option[Double]] =
        (for {
          priceHistory <- OptionT(
            dataFetcher
              .historicalPrices(equity, date.minusDays(parameters.horizon), date)
              .map(_.map(_.map(_.adjusted))))
        } yield standardDeviation(priceHistory)).run
    }
  }

  private def standardDeviation(priceHistory: Vector[Double]): Double = {
    val sum = priceHistory.sum
    val length = priceHistory.length
    val adjustedSumOfSquares =
      priceHistory.foldLeft(0.0)((acc, p) => acc + (p - sum / length) * (p - sum / length))

    sqrt(adjustedSumOfSquares / (length - 1))
  }
}
