package marketFactor

import akka.NotUsed
import akka.stream.scaladsl.Source

/**
  * Created by dennis on 16/8/16.
  */
trait MarketFactorsGenerator {
  def factors: Option[Source[MarketFactors, NotUsed]]
}
