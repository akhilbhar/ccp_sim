package marketFactor

import java.util.Calendar

import scalaz.Scalaz._
import scalaz._
import data.DataError
import instrument.{Equity, EquityOption, Instrument}
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.MarketFactorsGenerator.CurrentFactors
import model.Portfolio
import util.Math._
import util.Time.daysDiff

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.FutureInstances
import scalaz.{-\/, EitherT, \/, \/-}

/**
  * Builds market factors based on the data from the data fetcher.
  */
case object HistoricalMarketFactorsBuilder extends MarketFactorsBuilder with FutureInstances {
  override def ForecastMarketFactors(portfolio: Portfolio, date: Calendar, simulatedDays: Int)(
      implicit parameters: MarketFactorsParameters): Future[MarketFactorsGenerator] = {

    val instruments =
      portfolio.positions
        .map(_.instrument)
        .foldLeft(List.empty[Instrument])({
          case (acc, e: Equity) => e :: acc
          case (acc, eo: EquityOption) => eo :: eo.underlying :: acc
        })

    /* Current factors */
    val mapCurrentFactors: Map[Instrument, Future[DataError \/ CurrentFactors]] = instruments.map(instrument => {
      val from = date.clone().asInstanceOf[Calendar]
      from.add(Calendar.DATE, -parameters.horizon.toInt)
      val to = date.clone().asInstanceOf[Calendar]

      /* Build current factors for equity based on data from fetcher */
      val futureCurrentFactors = (
        for {
          price <- EitherT(instrument.historicalPrice(date)).map(_.adjusted)
          priceHistory <- EitherT(instrument.historicalPrices(from, to).map(_.map(_.map(_.adjusted))))
        } yield CurrentFactors(price, logVolatilityOfChange(priceHistory), priceHistory)
      ).run

      instrument -> futureCurrentFactors
    })(scala.collection.breakOut)

    val futureCurrentFactors = for {
      factors <- Future.sequence(mapCurrentFactors.map(entry => entry._2.map(i => (entry._1, i)))).map(_.toMap)
      filteredCurrentFactors = factors.collect {
        case (key, \/-(value)) => {
          (key, value)
        }
      }
    } yield filteredCurrentFactors

    for {
      currentFactors <- futureCurrentFactors
    } yield GBMMarketFactorsGenerator(date, simulatedDays, parameters.riskFreeRate, currentFactors)
  }

  override def marketFactors(date: Calendar)(implicit parameters: MarketFactorsParameters): MarketFactors = {
    new MarketFactors {
      override protected def price(instrument: Instrument): Future[MarketFactorsError \/ Double] = {
        EitherT(instrument.historicalPrice(date)).map(_.adjusted).leftMap(MissingData).run
      }

      override protected def volatility(instrument: Instrument): Future[MarketFactorsError \/ Double] = {
        val from = date.clone().asInstanceOf[Calendar]
        from.add(Calendar.DATE, -parameters.horizon.toInt)
        val to = date.clone().asInstanceOf[Calendar]

        (for {
          priceHistory <- EitherT(instrument.historicalPrices(from, to).map(_.map(_.map(_.adjusted))))
            .leftMap(MissingData)
        } yield logVolatilityOfChange(priceHistory)).run
      }

      override protected def mean(instrument: Instrument): Future[MarketFactorsError \/ Double] = {
        val from = Calendar.getInstance()
        val to = Calendar.getInstance()
        from.add(Calendar.DAY_OF_MONTH, -parameters.horizon.toInt)

        EitherT(instrument.historicalPrices(from, to))
          .map(_.map(_.adjusted))
          .map(meanOfChange)
          .leftMap(MissingData)
          .run
      }

      override protected def daysToMaturity(option: EquityOption): Future[MarketFactorsError \/ Double] =
        Future.successful({
          val now = Calendar.getInstance()

          val days = daysDiff(now, option.maturity)

          if (days > 0) \/-(days) else \/-(0)//-\/(ExpiredOption(option))
        })

      override protected def riskFreeRate: Future[MarketFactorsError \/ Double] =
        Future.successful(\/-(parameters.riskFreeRate))

      override protected def dividendYield(instrument: Instrument): Future[MarketFactorsError \/ Double] =
        EitherT(instrument.dividendYield).leftMap(MissingData).run
    }
  }
}
