package marketFactor
import java.util.Calendar

import akka.NotUsed
import akka.stream.scaladsl.Source
import breeze.linalg.{DenseMatrix, DenseVector, cholesky}
import breeze.numerics.exp
import breeze.stats.covmat
import breeze.stats.distributions.Gaussian
import instrument.Instrument
import marketFactor.MarketFactorsGenerator.CurrentFactors
import util.Math._
import util.Time.daysDiff

import scala.concurrent.Future
import scalaz.std.ListInstances

/**
  * Computes a one day simulation of the market factors using the Geometric Brownian Motion method.
  * @param date date until which data has to be taken in account.
  * @param rate risk free rate.
  * @param currentFactors the current market factors.
  */
case class OneDayGBMMarketFactorsGenerator(date: Calendar,
                                           rate: Double,
                                           currentFactors: Map[Instrument, CurrentFactors])
    extends MarketFactorsGenerator
    with ListInstances {
  override def factors: Source[MarketFactors, NotUsed] = {
    val generator = Gaussian(0, 1)
    val source: Source[Gaussian, NotUsed] = Source.repeat(generator)

    source.map(toMarketFactors)
  }

  /**
    * Transforms the random input in random market factors.
    * @param generator random generator from which to create market factors.
    * @return randomly generated market factors.
    */
  private def toMarketFactors(generator: Gaussian): MarketFactors = {
    val randomVector =
      DenseVector.tabulate(currentFactors.keys.size) { i =>
        generator.draw
      }

    val correlatedRandomValues = currentFactors.keys zip (choleskyFactorization * randomVector).toArray

    val generatedPrices: Map[Instrument, Double] =
      correlatedRandomValues.map({
        case (instrument, randomValue) =>
          (instrument, generatePrice(currentFactors(instrument), randomValue))
      })(scala.collection.breakOut)

    GeneratedMarketFactors(generatedPrices)
  }

  /**
    * Generates a run of a simulation using the closed form of the Geometric Brownian Motion process.
    * @param factors the current factors of the instrument.
    * @param randomValue random value
    * @return
    */
  private def generatePrice(factors: CurrentFactors, randomValue: Double): Double = {
    val currentPrice = factors.price
    val volatility = factors.volatility
    val historicalMean = meanOfChange(factors.priceHistory)

    currentPrice * exp((historicalMean - (volatility * volatility) / 2.0) + volatility * randomValue)
  }

  /**
    * Computes the cholesky factorization of the price historical in the current factors.
    * @return cholesky factorization of the historical prices
    */
  private val choleskyFactorization: DenseMatrix[Double] = {

    /**
      * Convert List of Option to Option of List
      * @param l list to convert
      * @tparam T type of what the options hold
      * @return Option of List
      */
    def sequence[T](l: List[Option[T]]): Option[List[T]] = {
      if (l.contains(None)) None else Some(l.flatten)
    }

    val priceHistory = currentFactors.values.toList.map(_.priceHistory.toList)

    val flatPriceHistory = priceHistory.flatten
    val cols = priceHistory.length
    val rows = flatPriceHistory.length / cols

    val priceMat = new DenseMatrix[Double](rows, cols, flatPriceHistory.toArray)

    cholesky(covmat(priceMat))
  }

  case class GeneratedMarketFactors(generatedPrices: Map[Instrument, Double]) extends MarketFactors {
    override protected def price(instrument: Instrument): Future[Option[Double]] = {
      Future.successful(generatedPrices.get(instrument))
    }

    override protected def volatility(instrument: Instrument): Future[Option[Double]] = {
      Future.successful(currentFactors.get(instrument).map(_.volatility))
    }

    override protected def mean(instrument: Instrument): Future[Option[Double]] = {
      Future.successful(currentFactors.get(instrument).map(_.priceHistory).map(meanOfChange))
    }

    override protected def daysToMaturity(maturity: Calendar): Future[Option[Double]] =
      Future.successful({
        val now = Calendar.getInstance()

        val days = daysDiff(now, maturity)
        val adjustedDays = days - 1

        if (adjustedDays > 0) Some(adjustedDays) else None
      })

    override protected def riskFreeRate: Future[Option[Double]] = Future.successful(Some(rate))

    override protected def dividendYield(instrument: Instrument): Future[Option[Double]] = instrument.dividendYield
  }
}
