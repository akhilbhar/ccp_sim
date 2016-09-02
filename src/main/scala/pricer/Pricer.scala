package pricer

import java.util.Calendar

import breeze.numerics.{exp, log, sqrt}
import breeze.stats.distributions.Gaussian
import marketFactor.MarketFactor.{DaysToMaturity, Price, RiskFreeRate, Volatility}
import marketFactor.{MarketFactor, MarketFactors}
import model._
import pricer.PricingError.MissingMarketFactors

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{-\/, EitherT, NonEmptyList, ValidationNel, \/, \/-}

/**
  * Prices instruments.
  */
@implicitNotFound(msg = "No pricer for instrument of type '${I}'.")
trait Pricer[I <: Instrument] {

  /**
    * Prices the instrument based on the market factors
    * @param instrument the instrument to be priced
    * @param factors the factors
    * @return
    */
  def price(instrument: I)(implicit factors: MarketFactors): Future[PricingError \/ Double]
}

object Pricer extends PricerImplicits

/**
  * Pricers based on the type of instrument.
  */
trait PricerImplicits {

  /**
    * Pricer for equity.
    */
  implicit object EquityPricer extends Pricer[Equity] {
    override def price(equity: Equity)(
        implicit factors: MarketFactors): Future[PricingError \/ Double] = {
      val priceFactor = Price(equity)

      /* Gets the price factor from the market factors */
      factors(priceFactor).map(
        _.map(p => \/-(p)).getOrElse(-\/(MissingMarketFactors(NonEmptyList(priceFactor)))))
    }
  }

  implicit object OptionPricer extends Pricer[EquityOption] {
    private[pricer] case class Parameters(daysToMaturity: Double,
                                          spot: Double,
                                          strike: Double,
                                          riskFreeRate: Double,
                                          volatility: Double) {
      def annualizedVolatility = volatility * sqrt(252.0)
      def timeToMaturity = daysToMaturity / 365.0
    }

    override def price(option: EquityOption)(
        implicit factors: MarketFactors): Future[PricingError \/ Double] = {

      def factor(factor: MarketFactor): Future[MissingMarketFactors \/ Double] = {
        factors(factor).map(
          _.map(f => \/-(f)).getOrElse(-\/(MissingMarketFactors(NonEmptyList(factor)))))
      }

      def parameters(underlying: Equity,
                     strike: Double,
                     maturity: Calendar): Future[PricingError \/ Parameters] = {
        val daysToMaturityF = factor(DaysToMaturity(maturity)).map(_.validation.toValidationNel)
        val spotF = factor(Price(underlying)).map(_.validation.toValidationNel)
        val riskFreeRateF = factor(RiskFreeRate).map(_.validation.toValidationNel)
        val volatilityF = factor(Volatility(underlying)).map(_.validation.toValidationNel)

        val parameters: Future[ValidationNel[MissingMarketFactors, Parameters]] = {
          for {
            daysToMaturity <- daysToMaturityF
            spot <- spotF
            riskFreeRate <- riskFreeRateF
            volatility <- volatilityF
          } yield {
            (daysToMaturity |@| spot |@| riskFreeRate |@| volatility) {
              case (days, spt, rate, vol) => Parameters(days, spt, strike, rate, vol)
            }
          }
        }

        parameters.map(
          _.fold(missingFactors => -\/(MissingMarketFactors(missingFactors.flatMap(_.factors))),
                 p => \/-(p)))
      }

      option match {
        case PutOption(underlying, strike, maturity) =>
          EitherT(parameters(underlying, strike, maturity)).map(put).run
        case CallOption(underlying, strike, maturity) =>
          EitherT(parameters(underlying, strike, maturity)).map(call).run
      }
    }

    private[this] def d1(parameters: Parameters): Double = {
      import parameters._

      val a = log(spot / strike) + (riskFreeRate + (annualizedVolatility * annualizedVolatility) / 2.0) * timeToMaturity
      val b = annualizedVolatility * sqrt(timeToMaturity)

      a / b
    }

    private[this] def d2(parameters: Parameters): Double = {
      import parameters._

      d1(parameters) - annualizedVolatility * sqrt(timeToMaturity)
    }

    private[this] def n(v: Double): Double = {
      Gaussian(0, 1).cdf(v)
    }

    private[this] def put(parameters: Parameters): Double = {
      import parameters._

      val d1_ = d1(parameters)
      val d2_ = d2(parameters)

      val a = n(-d2_) * strike * exp(-riskFreeRate * timeToMaturity)
      val b = n(-d1_) * spot

      a - b
    }

    private[this] def call(parameters: Parameters): Double = {
      import parameters._

      val p = Parameters(4, 210.59, 205, 0.002175, 0.008844)

      val ap = d1(p)
      val bp = d2(p)

      val d1_ = d1(parameters)
      val d2_ = d2(parameters)

      val a = n(d1_) * spot
      val b = n(d2_) * strike * exp(-riskFreeRate * timeToMaturity)

      a - b
    }
  }
}

sealed trait PricingError

object PricingError {
  case class MissingMarketFactors(factors: NonEmptyList[MarketFactor]) extends PricingError
}
