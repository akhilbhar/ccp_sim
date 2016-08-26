package marketFactor

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.concurrent.Future

/**
  * Created by dennis on 16/8/16.
  */
trait MarketFactorsGenerator {
  def factors: Source[Future[MarketFactors], NotUsed]
}
