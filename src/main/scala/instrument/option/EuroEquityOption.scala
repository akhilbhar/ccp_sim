package instrument.option
import java.util.Calendar

import data.{DataError, DataSource, GoogleSource}
import instrument.EquityOption.OptionType
import instrument.{Equity, EquityOption}
import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.PriceEntry
import pricingModel.equityOption.BlackSholes
import pricingModel.{ModelError, PricingModel}

import scala.concurrent.Future
import scalaz.\/

/**
  * Created by dennis on 3/9/16.
  */
case class EuroEquityOption(optionType: OptionType, underlying: Equity, strike: Double, maturity: Calendar)(
    implicit source: DataSource[EuroEquityOption] = GoogleSource,
    model: PricingModel[EuroEquityOption] = BlackSholes)
    extends EquityOption {
  override def price(implicit factors: MarketFactors,
                     factorsParameters: MarketFactorsParameters): Future[ModelError \/ Double] = model.price(this)

  override def historicalPrice(date: Calendar): Future[\/[DataError, PriceEntry]] = source.historicalPrice(this, date)

  override def historicalPrices(from: Calendar, to: Calendar): Future[\/[DataError, Vector[PriceEntry]]] =
    source.historicalPrices(this, from, to)

  override def dividendYield: Future[\/[DataError, Double]] = source.dividendYield(this)
}
