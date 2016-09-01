package marketFactor
import java.util.Calendar

import akka.NotUsed
import akka.stream.scaladsl.Source
import breeze.linalg.{DenseMatrix, DenseVector, cholesky}
import breeze.numerics.exp
import breeze.stats._
import breeze.stats.distributions.Gaussian
import marketFactor.MarketFactorsGenerator.CurrentFactors
import model.Equity
import util.Math._

import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.ListInstances

/**
  * Computes a one day simulation of the market factors using the Geometric Brownian Motion method.
  * @param date date until which data has to be taken in account.
  * @param rate risk free rate.
  * @param currentFactors the current market factors.
  */
case class OneDayGBMMarketFactorsGenerator(date: Calendar,
                                           rate: Double,
                                           currentFactors: Map[Equity, Option[CurrentFactors]])
    extends MarketFactorsGenerator
    with ListInstances {
  override def factors: Source[Option[MarketFactors], NotUsed] = {
    val generator = Gaussian(0, 1)
    val source: Source[Gaussian, NotUsed] = Source.repeat(generator)

    source.map(toMarketFactors)
  }

  /**
    * Transforms the random input in random market factors.
    * @param generator random generator from which to create market factors.
    * @return randomly generated market factors.
    */
  private def toMarketFactors(generator: Gaussian): Option[MarketFactors] = {
    val randomVector =
      DenseVector[Double]((1 to currentFactors.keys.size).map(_ => generator.draw).toArray)

    val correlatedRandomValuesO = for {
      choleskyFactorization <- choleskyFactorizationO
      randomCorrelatedVector = (choleskyFactorization * randomVector).toArray
    } yield currentFactors.keys zip randomCorrelatedVector

    val generatedPrices: Option[Map[Equity, Option[Double]]] =
      for (correlatedRandomValues <- correlatedRandomValuesO) yield {
        correlatedRandomValues.map({
          case (equity, randomValue) =>
            (equity, for {
              currentPrice <- currentFactors(equity).map(_.price)
              historicalPrices <- currentFactors(equity).map(_.priceHistory)
              historicalVolatily <- currentFactors(equity).map(_.volatility)
              historicalReturnMean = meanOfChange(historicalPrices)
            } yield generatePrice(currentPrice, historicalReturnMean, historicalVolatily, randomValue))
        })(scala.collection.breakOut)
      }

    generatedPrices.map(GeneratedMarketFactors)
  }

  /**
    * Generates a run of a simulation using the closed form of the Geometric Brownian Motion process.
    * @param currentPrice the current price of the asset.
    * @param historicalMean the historical mean return of the asset
    * @param volatility the volatility of the asset's return
    * @param randomValue random value
    * @return
    */
  private def generatePrice(currentPrice: Double,
                            historicalMean: Double,
                            volatility: Double,
                            randomValue: Double): Double = {
    currentPrice * exp((historicalMean - (volatility * volatility) / 2) + volatility * randomValue)
  }

  /**
    * Computes the cholesky factorization of the price historical in the current factors.
    * @return cholesky factorization of the historical prices
    */
  private val choleskyFactorizationO: Option[DenseMatrix[Double]] = {

    /**
      * Convert List of Option to Option of List
      * @param l list to convert
      * @tparam T type of what the options hold
      * @return Option of List
      */
    def sequence[T](l: List[Option[T]]): Option[List[T]] = {
      if (l.contains(None)) None else Some(l.flatten)
    }

    val priceHistoryOption = sequence(
      OptionT(currentFactors.values.toList).map(_.priceHistory.toList).run)

    for {
      priceHistory <- priceHistoryOption.map(_.flatten)
      cols <- priceHistoryOption.map(_.length)
      rows = priceHistory.length / cols

      priceMat = new DenseMatrix[Double](rows, cols, priceHistory.toArray)
      covMat = covmat(priceMat)
    } yield cholesky(covMat)
  }

  case class GeneratedMarketFactors(generatedPrices: Map[Equity, Option[Double]])
      extends MarketFactors {
    override protected def price(equity: Equity): Future[Option[Double]] = {
      Future.successful(generatedPrices.get(equity).flatten)
    }

    override protected def volatility(equity: Equity): Future[Option[Double]] = {
      Future.successful(currentFactors.get(equity).flatten.map(_.volatility))
    }

    override protected def daysToMaturity(maturity: Calendar): Future[Option[Double]] =
      Future.successful({
        val now = Calendar.getInstance()

        val milliseconds1: Long = now.getTimeInMillis
        val milliseconds2: Long = maturity.getTimeInMillis
        val diff: Long = milliseconds2 - milliseconds1
        val diffDays: Double = diff / (24.0 * 60.0 * 60.0 * 1000.0)
        val adjustedDiffDays = diffDays - 1

        if (adjustedDiffDays > 0) Some(adjustedDiffDays) else None
      })

    override protected def riskFreeRate: Future[Option[Double]] = Future.successful(Some(rate))
  }
}
