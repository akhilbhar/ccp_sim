package marketFactor

import akka.NotUsed
import akka.stream.scaladsl.Source

/**
  * Stream of market factors.
  */
trait MarketFactorsGenerator {

  def factors: Source[MarketFactors, NotUsed]

}

object MarketFactorsGenerator {

  case class CurrentFactors(price: Double, volatility: Double, priceHistory: Vector[Double])

}
