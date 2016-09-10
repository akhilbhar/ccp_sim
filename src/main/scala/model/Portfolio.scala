package model

import custodian.{Position, PositionError}
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import pricer._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{EitherT, NonEmptyList, \/}

case class Portfolio(positions: List[Position]) {
  def addPosition(position: Position): Portfolio = {
    Portfolio(position +: positions)
  }

  def removePosition(position: Position): Portfolio = {
    Portfolio(positions.filterNot(_.equals(position)))
  }

  def markToMarket(implicit factors: MarketFactors): Future[PortfolioError \/ Double] = {
    val total = for {
      prices <- Future.sequence(positions.map(_.markToMarket))
    } yield \/.fromEither(prices.map(_.validation.toValidationNel).reduceLeft((l, r) => (l |@| r)(_ + _)).toEither)

    EitherT(total).leftMap(UnderlyingMarkToMarketError).run
  }

  def price(implicit factors: MarketFactors,
            factorsParameters: MarketFactorsParameters): Future[PortfolioError \/ Double] = {
    val total = for {
      prices <- Future.sequence(positions.map(_.price))
    } yield \/.fromEither(prices.map(_.validation.toValidationNel).reduceLeft((l, r) => (l |@| r)(_ + _)).toEither)

    EitherT(total).leftMap(UnderlyingPricingError).run
  }
}

sealed trait PortfolioError
case class UnderlyingPricingError(errors: NonEmptyList[PositionError]) extends PortfolioError
case class UnderlyingMarkToMarketError(errors: NonEmptyList[PositionError]) extends PortfolioError
