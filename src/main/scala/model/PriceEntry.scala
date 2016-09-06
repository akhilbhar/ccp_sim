package model

import java.util.Calendar

/**
  * Created by dennis on 18/8/16.
  */
case class PriceEntry(date: Calendar,
                      open: Double,
                      high: Double,
                      low: Double,
                      close: Double,
                      volume: Long,
                      adjusted: Double)
