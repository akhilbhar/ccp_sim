package model

import java.time.LocalDate

/**
  * Created by dennis on 18/8/16.
  */
case class Price(date: LocalDate,
                 open: Double,
                 high: Double,
                 low: Double,
                 close: Double,
                 volume: Long,
                 adjusted: Double)
