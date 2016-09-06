package model

import custodian.Position
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import pricer._
import util.Math.sumList

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Portfolio(positions: List[Position]) extends MarkToMarket with PricingStrategy {
  def addPosition(position: Position): Portfolio = {
    Portfolio(position +: positions)
  }

  def removePosition(position: Position): Portfolio = {
    Portfolio(positions.filterNot(_.equals(position)))
  }

  override def markToMarket(implicit factors: MarketFactors): Future[Option[Double]] = {
    Future.sequence(positions.map(_.markToMarket)).map(sumList(_))
  }
  override def price(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]] = {
    Future.sequence(positions.map(_.price)).map(sumList(_))
  }
}
