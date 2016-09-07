package instrument.option

import java.util.Calendar

import instrument.Instrument
import instrument.equity.Equity
import model.PriceEntry
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Reads}
import play.libs.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

trait EquityOption extends Instrument {

  def optionType: OptionType
  def underlying: Equity
  def strike: Double
  def maturity: Calendar

  override def historicalPrice(date: Calendar): Future[Option[PriceEntry]] = Future {
    val ticker = underlying.ticker
    val day = maturity.get(Calendar.DAY_OF_MONTH)
    val month = maturity.get(Calendar.MONTH) + 1
    val year = maturity.get(Calendar.YEAR)

    val url = s"http://www.google.com/finance/option_chain?q=$ticker&expd=$day&expm=$month&expy=$year&output=json"

    val dirtyJsonString = Source.fromURL(url).mkString

    val regex1 = "(\\w+:)(\\d+\\.?\\d*)"
    val regex2 = "(\\w+):"

    val stillDirtyJSonString = dirtyJsonString.replaceAll(regex1, "$1\\\"$2\\\"")
    val jsonString = stillDirtyJSonString.replaceAll(regex2, "\\\"$1\\\":")

    println(url)
    println("---")
    println(dirtyJsonString)
    println("---")
    println(jsonString)

    val optionEntries_ = Json.parse(jsonString).findValue("calls").toString
    val optionEntries = play.api.libs.json.Json.parse(optionEntries_)

    implicit val optionEntryReads: Reads[OptionEntry] = (
      (JsPath \ "p").read[String] and
        (JsPath \ "strike").read[String]
    )(OptionEntry.apply _)

    val prices = optionEntries
      .validate[List[OptionEntry]]
      .fold(err => List.empty[Double], xs => {
        for {
          x <- xs
          if x.strike.toDouble == strike
        } yield x.p.toDouble
      })

    for {
      price <- prices.headOption // should be only one
    } yield PriceEntry(date, 0, 0, 0, price, 0, price)
  }

  override def historicalPrices(from: Calendar, to: Calendar): Future[Option[Vector[PriceEntry]]] = {
    Future.successful(None)
  }

  override def dividendYield: Future[Option[Double]] = Future.successful(None)

  private case class OptionEntry(p: String, strike: String)
}

sealed trait OptionType
case object Put extends OptionType
case object Call extends OptionType
