package marketFactor
import java.util.Calendar

import akka.NotUsed
import akka.stream.scaladsl.Source
import breeze.numerics.sqrt
import breeze.stats._
import breeze.stats.distributions.Gaussian
import marketFactor.MarketFactorsGenerator.CurrentFactors
import model.Equity

import scala.concurrent.Future

/**
  * Created by dennis on 29/8/16.
  */
case class OneDayGBMMarketFactorsGenerator(date: Calendar, currentFactors: Map[Equity, Option[CurrentFactors]])
    extends MarketFactorsGenerator {
  override def factors: Source[MarketFactors, NotUsed] = {
    val generator = Gaussian(0, 1)
    val source: Source[Gaussian, NotUsed] = Source.repeat(generator)

    source.map(toMarketFactors)
  }

  private def toMarketFactors(generator: Gaussian): MarketFactors = {
    val generatedPrices: Map[Equity, Option[Double]] = currentFactors.keys.map({ equity =>
      {
        (equity, for {
          currentPrice <- currentFactors(equity).map(_.price)
          volatility <- currentFactors(equity).map(_.volatility)
          historicalPrices <- currentFactors(equity).map(_.priceHistory)
          historicalMean = mean(historicalPrices)
          volatility <- currentFactors(equity).map(_.volatility)
        } yield generatePrice(currentPrice, historicalMean, volatility, generator.draw()))
      }
    })(scala.collection.breakOut)

    GeneratedMarketFactors(generatedPrices)
  }

  private def generatePrice(currentPrice: Double,
                            historicalMean: Double,
                            volatility: Double,
                            randomValue: Double): Double = {
    val change = currentPrice * (historicalMean * 1 + volatility * randomValue * sqrt(1))

    currentPrice + change
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
