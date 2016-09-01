package marketFactor

import akka.NotUsed
import akka.stream.scaladsl.Source
import spire.math.Real

/**
  * Stream of market factors.
  */
trait MarketFactorsGenerator {
  def factors: Source[Option[MarketFactors], NotUsed]
}

object MarketFactorsGenerator {
  case class CurrentFactors(price: Real, volatility: Real, priceHistory: Vector[Real])
}
