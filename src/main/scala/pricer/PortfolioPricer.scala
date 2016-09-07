//package pricer
//
//import marketFactor.MarketFactors
//import model._
//import instrument.equity.Equity
//import instrument.option.EquityOption
//import pricer.PortfolioPricingError.UnderlyingPricingErrors
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import scalaz.Scalaz._
//import scalaz.std.FutureInstances
//import scalaz.{-\/, EitherT, NonEmptyList, \/, \/-}
//
///**
//  * Prices portfolios.
//  */
////noinspection ScalaStyle
//case object PortfolioPricer extends FutureInstances {
//
//  /**
//    * Prices the given portfolio based on market factors.
//    * @param portfolio portfolio to be priced
//    * @param factors market factors
//    * @return the price of the portfolio or an error if it cannot price an underlying.
//    */
//  def price(portfolio: Portfolio)(implicit factors: MarketFactors): Future[PortfolioPricingError \/ Double] = {
//
//    /**
//      * Marks to market the instrument
//      * @param instrument instrument to be priced
//      * @param strategy pricing strategy for the given instrument
//      * @tparam I type of the instrument
//      * @return the price of instrument or an error if it cannot be priced
//      */
//    def markToMarket[I <: Instrument](instrument: I)(implicit strategy: Priceable[I]): Future[PricingError \/ Double] = {
//      Pricer(instrument)
//    }
//
//    /* List of the prices of each position in the portfolio or errors */
//    val mtmF: Future[List[PricingError \/ Double]] =
//      Future.sequence(portfolio.positions map {
//        case LongPos(instrument, volume, _) =>
//          (for { price <- EitherT(markToMarket(instrument)) } yield price * volume).run
//
//        case ShortPos(instrument, volume, _) =>
//          (for { price <- EitherT(markToMarket(instrument)) } yield -price * volume).run
//      })
//
//    /* Price of the portfolio or aggregated errors */
//    val totalF: Future[NonEmptyList[PricingError] \/ Double] = {
//      for {
//        mtm <- mtmF
//      } yield \/.fromEither(mtm.map(_.validation.toValidationNel).reduceLeft((l, r) => (l |@| r)(_ + _)).toEither)
//    }
//
//    /* Converts the list of errors to a portfolio pricing error */
//    for {
//      total <- totalF
//    } yield
//      total.fold(
//        error => -\/(UnderlyingPricingErrors(error.stream.toVector)),
//        success => \/-(success)
//      )
//  }
//}
//
//sealed trait PortfolioPricingError
//
//object PortfolioPricingError {
//
//  case class UnderlyingPricingErrors(errors: Vector[PricingError]) extends PortfolioPricingError
//
//}
