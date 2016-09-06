package model.option
import java.util.Calendar

import marketFactor.MarketFactors
import marketFactor.MarketFactorsBuilder.MarketFactorsParameters
import model.equity.Equity
import pricingModel.optionPricing.{BlackSholes, EquityOptionModel}

import scala.concurrent.Future

/**
  * Created by dennis on 3/9/16.
  */
case class EuroEquityOption(optionType: OptionType, underlying: Equity, strike: Double, maturity: Calendar)(
    implicit model: EquityOptionModel = BlackSholes)
    extends EquityOption {
  override def price(implicit factors: MarketFactors, factorsParameters: MarketFactorsParameters): Future[Option[Double]] = model.price(this)
}
