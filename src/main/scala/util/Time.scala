package util

import java.util.Calendar

/**
  * Created by dennis on 1/9/16.
  */
object Time {
  def daysDiff(from: Calendar, to: Calendar): BigDecimal = {
    val milliseconds1: Long = from.getTimeInMillis
    val milliseconds2: Long = to.getTimeInMillis
    val diff: Long = milliseconds2 - milliseconds1
    val diffDays = diff / BigDecimal(24.0 * 60.0 * 60.0 * 1000.0)

    diffDays
  }
}
