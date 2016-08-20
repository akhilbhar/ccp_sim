package model

import java.time.LocalDate

/**
  * Created by dennis on 18/8/16.
  */
case class Price(date: LocalDate,
                 open: BigDecimal,
                 high: BigDecimal,
                 low: BigDecimal,
                 close: BigDecimal,
                 volume: Long,
                 adjusted: BigDecimal)
