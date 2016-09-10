package pricingModel.equityOption

import breeze.numerics._
import instrument.EquityOption
import instrument.EquityOption.{Call, Put}
import marketFactor.MarketFactor._
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import pricingModel.{MissingMarketFactors, ModelError, PricingModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.Scalaz._
import scalaz.std.FutureInstances
import scalaz.{EitherT, \/}

/**
  * Created by dennis on 8/9/16.
  */
object Binomial extends PricingModel[EquityOption] with FutureInstances {
  override def price(option: EquityOption)(
      implicit factors: MarketFactors,
      factorsParameters: MarketFactorsParameters): Future[ModelError \/ Double] = {
    (for {
      param <- EitherT(parameters(option))
    } yield priceHelper(option, param, floor(param.daysToMaturity).toInt)).run
  }

  private def priceHelper(option: EquityOption, param: Parameters, n: Int): Double = {
    val t = param.timeToMaturity / n
    val up = exp(param.annualizedVolatility * sqrt(t))
    val down = 1.0 / up

    val scenarios = new Array[Double](n + 1)

    /*
    Option value at the end of different scenarios.
    scenario(x) -> x ups and (N - x) downs
     */
    for (i <- 0 to n) {
      option.optionType match {
        case Put => scenarios(i) = option.strike - param.underlyingPrice * pow(up, 2 * i - n)
        case Call => scenarios(i) = param.underlyingPrice * pow(up, 2 * i - n) - option.strike
      }

      if (scenarios(i) < 0) scenarios(i) = 0
    }

    val p = (exp((param.underlyingHistReturn - param.dividendYield) * t) - down) / (up - down) // -dividend

    val binomials = new Array[Double](n)

    for (i <- n - 1 to 0 by -1) {
      for (j <- 0 to i) {
        /*
                  scenario(i)
         *      p/          \*(1-p)
                /            \
           scenario(i)  scenario(i + 1)
         */
        scenarios(j) = (p * scenarios(j) + (1 - p) * scenarios(j + 1)) * exp(param.riskFreeRate * t)
        val value = option.optionType match {
          case Put => option.strike - pow(up, 2 * j - i)
          case Call => pow(up, 2 * j - i) - option.strike
        }

        if (scenarios(j) < value) scenarios(i) = value
      }
    }

    scenarios(0)
  }

  private case class Parameters(daysToMaturity: Double,
                                riskFreeRate: Double,
                                volatility: Double,
                                underlyingPrice: Double,
                                underlyingHistReturn: Double,
                                dividendYield: Double) {
    def annualizedVolatility = volatility * sqrt(365.0)
    def timeToMaturity = daysToMaturity / 365.0
  }

  private def parameters(option: EquityOption)(
      implicit factors: MarketFactors,
      parameters: MarketFactorsParameters): Future[ModelError \/ Parameters] = {
    for {
      daysToMaturity <- factors(DaysToMaturity(option))
      riskFreeRate <- factors(RiskFreeRate)
      volatility <- factors(Volatility(option.underlying))
      underlyingSpot <- factors(Price(option.underlying))
      underlyingHistReturn <- factors(Mean(option.underlying))
      dividendYield <- factors(DividendYield(option.underlying))
    } yield {
      \/.fromEither(
        (daysToMaturity.validation.toValidationNel |@|
          riskFreeRate.validation.toValidationNel |@|
          volatility.validation.toValidationNel |@|
          underlyingSpot.validation.toValidationNel |@|
          underlyingHistReturn.validation.toValidationNel |@|
          dividendYield.validation.toValidationNel)(Parameters).leftMap(MissingMarketFactors).toEither)
    }
  }
}
