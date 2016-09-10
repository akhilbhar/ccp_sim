package pricingModel.equityOption

import breeze.numerics._
import breeze.stats.distributions.Gaussian
import instrument.EquityOption
import instrument.EquityOption.{Call, Put}
import instrument.option.EuroEquityOption
import marketFactor.MarketFactor._
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import pricingModel.{MissingMarketFactors, ModelError, PricingModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.std.FutureInstances
import scalaz.{EitherT, \/}

/**
  * Created by dennis on 8/9/16.
  */
object BlackSholes extends PricingModel[EuroEquityOption] with FutureInstances {

  override def price(option: EuroEquityOption)(
      implicit factors: MarketFactors,
      factorsParameters: MarketFactorsParameters): Future[ModelError \/ Double] = {
    option.optionType match {
      case Put =>
        EitherT(parameters(option)).map(put(_)(option)).run
      case Call =>
        EitherT(parameters(option)).map(call(_)(option)).run
    }
  }

  private def d1(p: Parameters)(implicit option: EuroEquityOption): Double = {
    val a = log(p.spot / option.strike) + (p.riskFreeRate + (p.annualizedVolatility * p.annualizedVolatility) / 2.0) * p.timeToMaturity
    val b = p.annualizedVolatility * sqrt(p.timeToMaturity)

    a / b
  }

  private def d2(p: Parameters)(implicit option: EuroEquityOption): Double = {
    d1(p) - p.annualizedVolatility * sqrt(p.timeToMaturity)
  }

  private def n(v: Double): Double = {
    Gaussian(0, 1).cdf(v)
  }

  private def put(p: Parameters)(implicit option: EuroEquityOption): Double = {
    val d1_ = d1(p)
    val d2_ = d2(p)

    val a = n(-d2_) * option.strike * exp(-p.riskFreeRate * p.timeToMaturity)
    val b = n(-d1_) * p.spot

    a - b
  }

  private def call(p: Parameters)(implicit option: EuroEquityOption): Double = {
    val d1_ = d1(p)
    val d2_ = d2(p)

    val a = n(d1_) * p.spot
    val b = n(d2_) * option.strike * exp(-p.riskFreeRate * p.timeToMaturity)

    a - b
  }

  private case class Parameters(daysToMaturity: Double, spot: Double, riskFreeRate: Double, volatility: Double) {
    def annualizedVolatility = volatility * sqrt(365.0)
    def timeToMaturity = daysToMaturity / 365.0
  }

  private def parameters(option: EquityOption)(
      implicit factors: MarketFactors,
      parameters: MarketFactorsParameters): Future[ModelError \/ Parameters] = {
    for {
      daysToMaturity <- factors(DaysToMaturity(option))
      spot <- factors(Price(option.underlying))
      riskFreeRate <- factors(RiskFreeRate)
      volatility <- factors(Volatility(option.underlying))
    } yield {
      \/.fromEither(
        (daysToMaturity.validation.toValidationNel |@|
          spot.validation.toValidationNel |@|
          riskFreeRate.validation.toValidationNel |@|
          volatility.validation.toValidationNel)(Parameters).leftMap(MissingMarketFactors).toEither)
    }
  }
}
