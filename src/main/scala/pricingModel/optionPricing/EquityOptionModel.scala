package pricingModel.optionPricing

import java.util.Calendar

import breeze.numerics._
import breeze.stats.distributions.Gaussian
import marketFactor.MarketFactor.{DaysToMaturity, Price, RiskFreeRate, Volatility}
import marketFactor.{MarketFactor, MarketFactors}
import model.equity.Equity
import model.option._

import scala.concurrent.Future
import scalaz.OptionT
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.std.FutureInstances
import Helper.{Parameters, parameters}
import breeze.linalg.max
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import spire.std._
import util.Math.meanOfChange
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by dennis on 5/9/16.
  */
sealed trait EquityOptionModel {

  def price(option: EquityOption)(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]]

}

object BlackSholes extends EquityOptionModel with FutureInstances {

  override def price(option: EquityOption)(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]] = {
    option.optionType match {
      case Put =>
        OptionT(parameters(option)).map(put).run
      case Call =>
        OptionT(parameters(option)).map(call).run
    }
  }

  private def d1(p: Parameters): Double = {

    val a = log(p.spot / p.strike) + (p.riskFreeRate + (p.annualizedVolatility * p.annualizedVolatility) / 2.0) * p.timeToMaturity
    val b = p.annualizedVolatility * sqrt(p.timeToMaturity)

    a / b
  }

  private def d2(p: Parameters): Double = {
    d1(p) - p.annualizedVolatility * sqrt(p.timeToMaturity)
  }

  private def n(v: Double): Double = {
    Gaussian(0, 1).cdf(v)
  }

  private def put(p: Parameters): Double = {
    val d1_ = d1(p)
    val d2_ = d2(p)

    val a = n(-d2_) * p.strike * exp(-p.riskFreeRate * p.timeToMaturity)
    val b = n(-d1_) * p.spot

    a - b
  }

  private def call(p: Parameters): Double = {
    val d1_ = d1(p)
    val d2_ = d2(p)

    val a = n(d1_) * p.spot
    val b = n(d2_) * p.strike * exp(-p.riskFreeRate * p.timeToMaturity)

    a - b
  }

}

object Binomial extends EquityOptionModel with FutureInstances {
  override def price(option: EquityOption)(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]] = {
    (for {
      param <- OptionT(parameters(option))
    } yield priceHelper(option, param, 100)).run
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
        case Put => scenarios(i) = param.strike - param.underlyingPrice * pow(up, 2 * i - n)
        case Call => scenarios(i) = param.underlyingPrice * pow(up, 2 * i - n) - param.strike
      }

      if (scenarios(i) < 0) scenarios(i) = 0
    }

    val p = (exp(param.underlyingHistReturn * t) - down) / (up - down)

    val binomials = new Array[Double](n)

    for (i <- n - 1 to 0 by -1) {
      for (j <- 0 to i) {
        /*
                  scenario(i)
               *p/          \*(1-p)
                /            \
           scenario(i)  scenario(i + 1)
         */
        scenarios(j) = (p * scenarios(j) + (1 - p) * scenarios(j + 1)) * exp(param.riskFreeRate * t)
        val value = option.optionType match {
          case Put => param.strike - pow(up, 2 * j - i)
          case Call => pow(up, 2 * j - i) - param.strike
        }

        if (scenarios(j) < value) scenarios(i) = value
      }
    }

    scenarios(0)
  }
}

//  private def buildUnderlyingTree(p: Parameters): Tree = {
//    def buildTreeHelper(value: Double, previous: Double, up: Double, down: Double, daysLeft: Long): Tree = {
//      daysLeft match {
//        case 0 => Leaf(value)
//        case _ => {
//          val downTree = buildTreeHelper(previous * down, previous, up, down, daysLeft - 1)
//          val upTree = buildTreeHelper(previous * up, previous, up, down, daysLeft - 1)
//          Node(value, downTree, upTree)
//        }
//      }
//    }
//
//    val n =
//    val up = exp(p.annualizedVolatility * sqrt(1.0 / 365.0))
//    val down = 1.0 / up
//    val daysLeft = floor(p.daysToMaturity).toLong
//
//    buildTreeHelper(p.underlyingPrice, p.underlyingPrice, up, down, daysLeft)
//  }
//
//  private def buildOptionTree(option: EquityOption, p: Parameters): Tree = {
//    val underlyingTree = buildUnderlyingTree(p)
//
//    def buildOptionTreeHelper(tree: Tree): Tree = {
//      tree match {
//        case Leaf(value) => {
//          option.optionType match {
//            case Call => Leaf(max(value - option.strike, 0))
//            case Put => Leaf(max(option.strike - value, 0))
//          }
//        }
//        case Node(value, down, up) => Node(value, buildOptionTreeHelper(down), buildOptionTreeHelper(up))
//      }
//    }
//
//    buildOptionTreeHelper(underlyingTree)
//  }
//}

//  private def build
//
////  private def buildTree(p: Parameters) = {
////    val daysLeft = floor(p.daysToMaturity).toLong
////
////    def buildTreeHelper(daysLeft: Long, p: Parameters, prev: Double, f: Double => Double): Tree = {
////      daysLeft match {
////        case 0 => Leaf(f(prev))
////        case _ => {
////          val down = buildTreeHelper(daysLeft - 1, p, prev, {_ * exp(-p.volatility * sqrt(daysLeft - 1))})
////          val up = buildTreeHelper(daysLeft - 1, p, prev, {_ * exp(p.volatility * sqrt(daysLeft - 1))})
////
////          Node(f(prev), down, up)
////        }
////      }
////    }
////
////    def down(x: Double) = x * exp(-p.annualizedVolatility * sqrt(1.0/365.0))
////    def up(x: Double) = x * exp(p.annualizedVolatility * sqrt(1.0/365.0))
////
////    bla(daysLeft, p, 50, down, up)
////  }
//
//  private sealed trait Tree
//  private case class Node(value: Double, down: Tree, up: Tree) extends Tree
//  private case class Leaf(value: Double) extends Tree


object Helper extends FutureInstances {
  case class Parameters(daysToMaturity: Double,
                        spot: Double,
                        strike: Double,
                        riskFreeRate: Double,
                        volatility: Double,
                        underlyingPrice: Double,
                        underlyingHistReturn: Double) {
    def annualizedVolatility = volatility * sqrt(252.0)
    def timeToMaturity = daysToMaturity / 365.0
  }

  def parameters(option: EquityOption)(implicit factors: MarketFactors, parameters: MarketFactorsParameters): Future[Option[Parameters]] = {
    val daysToMaturityF = factors(DaysToMaturity(option.maturity))
    val spotF = factors(Price(option.underlying))
    val riskFreeRateF = factors(RiskFreeRate)
    val volatilityF = factors(Volatility(option.underlying))
    val underlyingPriceF = factors(Price(option.underlying))

    val from = Calendar.getInstance()
    val to = Calendar.getInstance()
    from.add(Calendar.DAY_OF_MONTH, -parameters.horizon.toInt)

    val underlyingHistF = OptionT(option.underlying.historicalPrices(from, to)).map(_.map(_.adjusted)).run

    {
      for {
        daysToMaturity <- OptionT(daysToMaturityF)
        spot <- OptionT(spotF)
        riskFreeRate <- OptionT(riskFreeRateF)
        volatility <- OptionT(volatilityF)
        underlyingPrice <- OptionT(underlyingPriceF)
        underlyingHistReturn <- OptionT(underlyingHistF).map(meanOfChange)
      } yield Parameters(daysToMaturity, spot, option.strike, riskFreeRate, volatility, underlyingPrice, underlyingHistReturn)
    }.run
  }
}
