package marketFactor
import java.util.Calendar

import akka.NotUsed
import akka.stream.scaladsl.Source
import breeze.linalg.{DenseMatrix, DenseVector, cholesky}
import breeze.numerics.sqrt
import breeze.stats._
import breeze.stats.distributions.Gaussian
import marketFactor.MarketFactorsGenerator.CurrentFactors
import model.Equity

import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.ListInstances

/**
  * Created by dennis on 29/8/16.
  */
case class OneDayGBMMarketFactorsGenerator(date: Calendar,
                                           currentFactors: Map[Equity, Option[CurrentFactors]])
    extends MarketFactorsGenerator
    with ListInstances {
  override def factors: Source[Option[MarketFactors], NotUsed] = {
    val generator = Gaussian(0, 1)
    val source: Source[Gaussian, NotUsed] = Source.repeat(generator)

    source.map(toMarketFactors)
  }

  private def toMarketFactors(generator: Gaussian): Option[MarketFactors] = {
    val randomVector =
      DenseVector[Double]((1 to currentFactors.keys.size).map(_ => generator.draw).toArray)

    val randomCorrelatedVectorO = for {
      choleskyFactorization <- choleskyFactorizationO
    } yield choleskyFactorization * randomVector

    val correlatedRandomValuesO = for {
      randomCorrelatedVector <- randomCorrelatedVectorO.map(_.toArray)
    } yield currentFactors.keys zip randomCorrelatedVector

    val generatedPrices: Option[Map[Equity, Option[Double]]] =
      for (correlatedRandomValues <- correlatedRandomValuesO) yield {
        correlatedRandomValues.map({
          case (equity, randomValue) =>
            (equity, for {
              currentPrice <- currentFactors(equity).map(_.price)
              historicalPrices <- currentFactors(equity).map(_.priceHistory)
              historicalReturnMean = meanOfChange(historicalPrices)
              historicalReturnVolatility = volatilityOfChange(historicalPrices)
            } yield generatePrice(currentPrice, historicalReturnMean, historicalReturnVolatility, randomValue))
        })(scala.collection.breakOut)
      }

    generatedPrices.map(GeneratedMarketFactors)
  }

  private def generatePrice(currentPrice: Double,
                            historicalMean: Double,
                            volatility: Double,
                            randomValue: Double): Double = {
    val change = currentPrice * (historicalMean * 1 + volatility * randomValue * sqrt(1))

    currentPrice + change
  }

  private def change(data: Vector[Double]): Vector[Double] = {
    data
      .sliding(2)
      .map {
        case Seq(a, b, _ *) => (a - b) / b
      }
      .toVector
  }

  private def meanOfChange(data: Vector[Double]): Double = mean(change(data))

  private def volatilityOfChange(data: Vector[Double]): Double = stddev(change(data))

  private def choleskyFactorizationO: Option[DenseMatrix[Double]] = {

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
      covMat = covmat(priceMat) // new DenseMatrix[Double](2,2, Array(1.0,0,0,1.0))
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
  }
}
