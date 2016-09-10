package instrument

import java.util.Calendar

import data._
import instrument.EquityOption.OptionType
import marketFactor.MarketFactor.Price
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import marketFactor.{MarketFactors, MarketFactorsError}
import model.PriceEntry
import play.api.libs.json.{JsPath, Reads}
import play.libs.Json
import pricingModel.equity.IdentityModel
import pricingModel.{ModelError, PricingModel}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, \/, \/-}

/**
  * Created by dennis on 7/9/16.
  */
sealed trait Instrument {
  def historicalPrice(date: Calendar): Future[\/[DataError, PriceEntry]]
  def historicalPrices(from: Calendar, to: Calendar): Future[\/[DataError, Vector[PriceEntry]]]
  def dividendYield: Future[DataError \/ Double]

  def price(implicit factors: MarketFactors, parameters: MarketFactorsParameters): Future[ModelError \/ Double]
  def markToMarket(implicit factors: MarketFactors): Future[MarketFactorsError \/ Double] = factors(Price(this))
}

/* Equity */
final case class Equity(ticker: String)(implicit source: DataSource[Equity] = YahooDataSource,
                                  model: PricingModel[Equity] = IdentityModel)
    extends Instrument {

  override def price(implicit factors: MarketFactors,
                     factorsParameters: MarketFactorsParameters): Future[ModelError \/ Double] = {
    model.price(this)
  }

  override def historicalPrice(date: Calendar): Future[DataError \/ PriceEntry] =
    source.historicalPrice(this, date)

  override def historicalPrices(from: Calendar, to: Calendar): Future[DataError \/ Vector[PriceEntry]] =
    source.historicalPrices(this, from, to)

  override def dividendYield: Future[DataError \/ Double] = source.dividendYield(this)
}

trait EquityOption extends Instrument {

  def optionType: OptionType
  def underlying: Equity
  def strike: Double
  def maturity: Calendar

}

object EquityOption {
  sealed trait OptionType
  case object Put extends OptionType
  case object Call extends OptionType
}
