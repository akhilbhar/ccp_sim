package model.option

import java.util.Calendar

import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import model.equity.Equity
import model.{Instrument, PriceEntry}
import play.api.libs.json.{JsError, JsPath, JsSuccess, Reads}
import play.libs.Json

import scala.concurrent.Future
import scala.io.Source

trait EquityOption extends Instrument {

  def optionType: OptionType
  def underlying: Equity
  def strike: Double
  def maturity: Calendar

  override def historicalPrice(date: Calendar): Future[Option[PriceEntry]] = {
//    val ticker = underlying.ticker
//    val day = maturity.get(Calendar.DAY_OF_MONTH)
//    val month = maturity.get(Calendar.MONTH) + 1
//    val year = maturity.get(Calendar.YEAR)
//
//    val url = s"http://www.google.com/finance/option_chain?q=$ticker&expd=$day&expm=$month&expy=$year&output=json"
//
//    val dirtyJsonString = Source.fromURL(url).mkString
//
//    val regex1 = "(\\w+:)(\\d+\\.?\\d*)"
//    val regex2 = "(\\w+):"
//
//    val stillDirtyJSonString = dirtyJsonString.replaceAll(regex1, "$1\\\"$2\\\"")
//    val jsonString = stillDirtyJSonString.replaceAll(regex2, "\\\"$1\\\":")
//
//    val optionEntries_ = Json.parse(jsonString).findValue("calls").toString
//    val optionEntries = play.api.libs.json.Json.parse(optionEntries_)
//
//    implicit val optionEntryReads: Reads[OptionEntry] = (
//      (JsPath \ "cid").read[String] and
//        (JsPath \ "name").read[String] and
//        (JsPath \ "s").read[String] and
//        (JsPath \ "e").read[String] and
//        (JsPath \ "p").read[String] and
//        (JsPath \ "cs").read[String] and
//        (JsPath \ "c").read[String] and
//        (JsPath \ "cp").read[String] and
//        (JsPath \ "b").read[String] and
//        (JsPath \ "a").read[String] and
//        (JsPath \ "oi").read[String] and
//        (JsPath \ "vol").read[String] and
//        (JsPath \ "strike").read[String] and
//        (JsPath \ "expiry").read[String]
//    )(OptionEntry.apply _)
//
////    println(optionEntries.toString())
//
//    println(optionEntries.validate[List[OptionEntry]])
//
//    val prices = optionEntries
//      .validate[List[OptionEntry]]
//      .fold(err => List.empty[Double], xs => {
//        for {
//          x <- xs
//          if x.strike.toDouble == strike
//        } yield x.p.toDouble
//      })
//
//    println(prices.headOption)

    Future.successful(None)
  }

  override def historicalPrices(from: Calendar, to: Calendar): Future[Option[Vector[PriceEntry]]] = {
    Future.successful(None)
  }

  private case class OptionEntry(cid: String,
                                 name: String,
                                 s: String,
                                 e: String,
                                 p: String,
                                 cs: String,
                                 c: String,
                                 cp: String,
                                 b: String,
                                 a: String,
                                 oi: String,
                                 vol: String,
                                 strike: String,
                                 expiry: String)
}

sealed trait OptionType
case object Put extends OptionType
case object Call extends OptionType
