package data

import java.util.Calendar

import instrument.EquityOption
import model.PriceEntry
import play.api.libs.json.{JsPath, Reads}
import play.libs.Json

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, \/, \/-}
import play.api.libs.functional.syntax._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by dennis on 10/9/16.
  */
object GoogleSource extends DataSource[EquityOption] {
  private val memoizedPrices = new TrieMap[(EquityOption, Calendar), Future[DataError \/ PriceEntry]]

  override def historicalPrice(option: EquityOption, date: Calendar): Future[\/[DataError, PriceEntry]] =
    memoizedPrices.getOrElseUpdate((option, date), Future {
      val ticker = option.underlying.ticker
      val day = option.maturity.get(Calendar.DAY_OF_MONTH)
      val month = option.maturity.get(Calendar.MONTH) + 1
      val year = option.maturity.get(Calendar.YEAR)

      Try {
        val url = s"http://www.google.com/finance/option_chain?q=$ticker&expd=$day&expm=$month&expy=$year&output=json"

        val dirtyJsonString = Source.fromURL(url).mkString

        val regex1 = "(\\w+:)(\\d+\\.?\\d*)"
        val regex2 = "(\\w+):"

        val stillDirtyJSonString = dirtyJsonString.replaceAll(regex1, "$1\\\"$2\\\"")
        val jsonString = stillDirtyJSonString.replaceAll(regex2, "\\\"$1\\\":")

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
              if x.strike.toDouble == option.strike
            } yield x.p.toDouble
          })

        for {
          price <- prices.headOption // should be only one
        } yield PriceEntry(date, 0, 0, 0, price, 0, price)
      } match {
        case Success(priceEntry) =>
          priceEntry match {
            case Some(v) => \/-(v)
            case None => -\/(DataNotFound(option))
          }
        case Failure(e) => -\/(DataFetchingError(e))
      }
    })

  private case class OptionEntry(p: String, strike: String)

  override def historicalPrices(instrument: EquityOption,
                                from: Calendar,
                                to: Calendar): Future[\/[DataError, Vector[PriceEntry]]] = {
    Future.successful(-\/(DataNotFound(instrument)))
  }

  override def dividendYield(instrument: EquityOption): Future[\/[DataError, Double]] =
    Future.successful(-\/(DataFetchingError(new Throwable("No dividend for options."))))
}
