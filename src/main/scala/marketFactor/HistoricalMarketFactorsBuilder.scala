package marketFactor

import java.util.Calendar

import instrument.Instrument
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.MarketFactorsGenerator.CurrentFactors
import model.Portfolio
import instrument.equity.Equity
import instrument.option.EquityOption
import util.Math._
import util.Time.daysDiff

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.FutureInstances

/**
  * Builds market factors based on the data from the data fetcher.
  */
case object HistoricalMarketFactorsBuilder extends MarketFactorsBuilder with FutureInstances {
  override def oneDayForecastMarketFactors(portfolio: Portfolio, date: Calendar)(
      implicit parameters: MarketFactorsParameters): Future[MarketFactorsGenerator] = {

    val instruments =
      portfolio.positions
        .map(_.instrument)
        .foldLeft(List.empty[Instrument])({
          case (acc, e: Equity) => e :: acc
          case (acc, eo: EquityOption) => eo :: eo.underlying :: acc
          case (_, _) => Nil
        })

    /* Current factors */
    val mapCurrentFactors: Map[Instrument, Future[Option[CurrentFactors]]] = instruments.map(instrument => {
      val from = date.clone().asInstanceOf[Calendar]
      from.add(Calendar.DATE, -parameters.horizon.toInt)
      val to = date.clone().asInstanceOf[Calendar]

      /* Build current factors for equity based on data from fetcher */
      val futureCurrentFactors = (
        for {
          price <- OptionT(instrument.historicalPrice(date)).map(_.adjusted)
          priceHistory <- OptionT(instrument.historicalPrices(from, to).map(_.map(_.map(_.adjusted))))
        } yield CurrentFactors(price, logVolatilityOfChange(priceHistory), priceHistory)
      ).run

      instrument -> futureCurrentFactors
    })(scala.collection.breakOut)

    val futureCurrentFactors = for {
      currentFactors <- Future.sequence(mapCurrentFactors.map(entry => entry._2.map(i => (entry._1, i)))).map(_.toMap)
      filteredCurrentFactors = currentFactors.collect {
        case (key, Some(value)) => (key, value)
      }
    } yield filteredCurrentFactors

    for {
      currentFactors <- futureCurrentFactors
    } yield OneDayGBMMarketFactorsGenerator(date, parameters.riskFreeRate, currentFactors)
  }

  override def marketFactors(date: Calendar)(implicit parameters: MarketFactorsParameters): MarketFactors = {
    new MarketFactors {
      override protected def price(instrument: Instrument): Future[Option[Double]] = {
        OptionT(instrument.historicalPrice(date)).map(_.adjusted).run
      }

      override protected def volatility(instrument: Instrument): Future[Option[Double]] = {
        val from = date.clone().asInstanceOf[Calendar]
        from.add(Calendar.DATE, -parameters.horizon.toInt)
        val to = date.clone().asInstanceOf[Calendar]

        (for {
          priceHistory <- OptionT(instrument.historicalPrices(from, to).map(_.map(_.map(_.adjusted))))
        } yield logVolatilityOfChange(priceHistory)).run
      }

      override protected def mean(instrument: Instrument): Future[Option[Double]] = {
        val from = Calendar.getInstance()
        val to = Calendar.getInstance()
        from.add(Calendar.DAY_OF_MONTH, -parameters.horizon.toInt)

        OptionT(instrument.historicalPrices(from, to)).map(_.map(_.adjusted)).map(meanOfChange).run
      }

      override protected def daysToMaturity(maturity: Calendar): Future[Option[Double]] =
        Future.successful({
          val now = Calendar.getInstance()

          val days = daysDiff(now, maturity)

          if (days > 0) Some(days) else None
        })

      override protected def riskFreeRate: Future[Option[Double]] =
        Future.successful(Some(parameters.riskFreeRate))

      override protected def dividendYield(instrument: Instrument): Future[Option[Double]] = instrument.dividendYield
    }
  }
}
