package marketFactor

import java.util.Calendar

import akka.NotUsed
import akka.stream.scaladsl.Source
import marketFactor.OneDayForecastMarketFactorsGenerator.CurrentFactors
import model.Equity
import org.apache.commons.math3.random.{CorrelatedRandomVectorGenerator, GaussianRandomGenerator, JDKRandomGenerator}
import org.apache.commons.math3.stat.correlation.Covariance

import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.ListInstances

/**
  * Created by dennis on 20/8/16.
  */
case class OneDayMarketForecastFactorsGenerator(
    date: Calendar,
    currentFactors: Map[Equity, Option[CurrentFactors]])
    extends MarketFactorsGenerator with ListInstances {

  // private val nextDay = date.plusDays(1)

  override def factors: Option[Source[MarketFactors, NotUsed]] = {
    generator.map(createSource)
  }

  private def generator: Option[CorrelatedRandomVectorGenerator] = {
    val randomGenerator = new JDKRandomGenerator

    val gaussianRandomGenerator = new GaussianRandomGenerator(randomGenerator)

    val foo = currentFactors.values

    val priceHistoryOptionList = sequence(
      OptionT(currentFactors.values.toList).map(_.priceHistory.toList).run)

    val priceHistoryOption = priceHistoryOptionList.map(_.map(_.toArray).toArray)

    val covarianceMatrixOption = for {
      priceHistory <- priceHistoryOption
    } yield new Covariance(priceHistory).getCovarianceMatrix

    for {
      covarianceMatrix <- covarianceMatrixOption
    } yield
      new CorrelatedRandomVectorGenerator(covarianceMatrix,
                                          1.0e-12 * covarianceMatrix.getNorm,
                                          gaussianRandomGenerator)
  }

  private def sequence[T](l: List[Option[T]]): Option[List[T]] = {
    if (l.contains(None)) None else Some(l.flatten)
  }

  private def createSource(
      generator: CorrelatedRandomVectorGenerator): Source[MarketFactors, NotUsed] = {
    Source
      .repeat(generator)
      .map(random => {
        val correlated = currentFactors.keys zip random.nextVector()

        val generatedPrice: Map[Equity, Option[Double]] = correlated.toSeq.map({
          case (equity, randomValue) =>
            (equity, for {
              price <- currentFactors(equity).map(_.price)
              volatility <- currentFactors(equity).map(_.volatility)
            } yield generatePrice(price, volatility, randomValue))
        })(scala.collection.breakOut)

        GeneratedMarketFactors(generatedPrice)
      })
  }

  private def generatePrice(price: Double, volatility: Double, randomValue: Double): Double = {
    price + volatility * randomValue
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

object OneDayForecastMarketFactorsGenerator {

  sealed trait DataError

  case class CurrentFactors(price: Double, volatility: Double, priceHistory: Vector[Double])

  case class MissingData(err: Throwable) extends DataError

}
