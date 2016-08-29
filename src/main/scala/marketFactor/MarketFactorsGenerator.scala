package marketFactor

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.apache.commons.math3.random.CorrelatedRandomVectorGenerator

/**
  * Stream of market factors.
  */
trait MarketFactorsGenerator {
  def factors: Option[Source[CorrelatedRandomVectorGenerator, NotUsed]#Repr[MarketFactors]]
}
