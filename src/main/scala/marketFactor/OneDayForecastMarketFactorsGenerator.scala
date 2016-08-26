package marketFactor

import java.time.LocalDate

import akka.NotUsed
import akka.stream.scaladsl.Source
import marketFactor.OneDayForecastMarketFactorsGenerator.CurrentFactors
import model.Equity
import org.apache.commons.math3.random.{CorrelatedRandomVectorGenerator, GaussianRandomGenerator, JDKRandomGenerator}
import org.apache.commons.math3.stat.correlation.Covariance

import scalaz.OptionT

/**
  * Created by dennis on 20/8/16.
  */
case class OneDayMarketForecastFactorsGenerator(
    date: LocalDate,
    currentFactors: Map[Equity, Option[CurrentFactors]])
    extends MarketFactorsGenerator {

  private val nextDay = date.plusDays(1)

  override def factors: Option[Source[MarketFactors, NotUsed]] = {
    generator.map(createSource)
  }

  private def generator: Option[CorrelatedRandomVectorGenerator] = {
    val randomGenerator = new JDKRandomGenerator

    val gaussianRandomGenerator = new GaussianRandomGenerator(randomGenerator)

    val priceHistoryOption = sequence(
      OptionT(currentFactors.values).map(_.priceHistory.toArray).run.toArray)

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

  private def sequence[T](l: Array[Option[T]]): Option[Array[T]] = {
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
    override protected def price(equity: Equity): Option[Double] = {
      generatedPrices.get(equity).flatten
    }

    override protected def volatility(equity: Equity): Option[Double] = {
      currentFactors.get(equity).flatten.map(_.volatility)
    }
  }
}

object OneDayForecastMarketFactorsGenerator {

  sealed trait DataError

  case class CurrentFactors(price: Double, volatility: Double, priceHistory: Vector[Double])

  case class MissingData(err: Throwable) extends DataError

}
