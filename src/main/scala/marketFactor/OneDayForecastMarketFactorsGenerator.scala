//package marketFactor
//
//import java.util.Calendar
//
//import akka.NotUsed
//import akka.stream.scaladsl.{Flow, Sink, Source}
//import marketFactor.MarketFactorsGenerator.CurrentFactors
//import model.equity.Equity
//import org.apache.commons.math3.random.{
//  CorrelatedRandomVectorGenerator,
//  GaussianRandomGenerator,
//  JDKRandomGenerator
//}
//import org.apache.commons.math3.stat.correlation.Covariance
//
//import scala.concurrent.Future
//import scalaz.OptionT
//import scalaz.std.ListInstances
//
///**
//  * Generates a stream of one-day predicted market factors.
//  * @param date date until data is used.
//  * @param currentFactors
//  */
//case class OneDayMarketForecastFactorsGenerator(
//    date: Calendar,
//    currentFactors: Map[Equity, Option[CurrentFactors]])
//    extends MarketFactorsGenerator
//    with ListInstances {
//
//  // private val nextDay = date.plusDays(1)
//
//  override def factors: Option[
//    Source[CorrelatedRandomVectorGenerator, NotUsed]#Repr[MarketFactors]] = {
//    val source = for { g <- generator } yield Source.repeat(g)
//
//    val marketFactors = Flow[CorrelatedRandomVectorGenerator].map(toMarketFactors)
//
//    source.map(_.via(marketFactors))
//  }
//
//  /**
//    * Generator of vectors with the same correlations as between the factors.
//    */
//  private def generator: Option[CorrelatedRandomVectorGenerator] = {
//    val randomGenerator = new JDKRandomGenerator
//
//    val gaussianRandomGenerator = new GaussianRandomGenerator(randomGenerator)
//
//    val priceHistoryOptionList = sequence(
//      OptionT(currentFactors.values.toList).map(_.priceHistory.toList).run)
//
//    // Convert to arrays
//    val priceHistoryOption = priceHistoryOptionList.map(_.map(_.toArray).toArray)
//
//    val covarianceMatrixOption = for {
//      priceHistory <- priceHistoryOption
//    } yield new Covariance(priceHistory).getCovarianceMatrix
//
//    for {
//      covarianceMatrix <- covarianceMatrixOption
//    } yield
//      new CorrelatedRandomVectorGenerator(covarianceMatrix,
//                                          1.0e-12 * covarianceMatrix.getNorm,
//                                          gaussianRandomGenerator)
//  }
//
//  // TODO should move this?
//  /**
//    * Convert List of Option to Option of List
//    * @param l list to convert
//    * @tparam T type of what the options hold
//    * @return Option of List
//    */
//  private def sequence[T](l: List[Option[T]]): Option[List[T]] = {
//    if (l.contains(None)) None else Some(l.flatten)
//  }
//
//  private def toMarketFactors(random: CorrelatedRandomVectorGenerator): MarketFactors = {
//    // Equity and their random value
//    val correlated = currentFactors.keys zip random.nextVector()
//
//    val generatedPrices: Map[Equity, Option[Double]] = correlated.toSeq.map({
//      case (equity, randomValue) =>
//        (equity, for {
//          price <- currentFactors(equity).map(_.price)
//          volatility <- currentFactors(equity).map(_.volatility)
//        } yield generatePrice(price, volatility, randomValue))
//    })(scala.collection.breakOut)
//
//    GeneratedMarketFactors(generatedPrices)
//  }
//
//  /**
//    * Predicts price
//    */
//  private def generatePrice(price: Double, volatility: Double, randomValue: Double): Double = {
//    println(s"Price: $price")
//    println(s"Volatility: $volatility")
//    println(s"Random: $randomValue")
//
//    price + volatility * randomValue
//  }
//
//  case class GeneratedMarketFactors(generatedPrices: Map[Equity, Option[Double]])
//      extends MarketFactors {
//    override protected def price(equity: Equity): Future[Option[Double]] = {
//      Future.successful(generatedPrices.get(equity).flatten)
//    }
//
//    override protected def volatility(equity: Equity): Future[Option[Double]] = {
//      Future.successful(currentFactors.get(equity).flatten.map(_.volatility))
//    }
//  }
//}
