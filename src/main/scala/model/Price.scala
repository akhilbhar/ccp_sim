package model

import java.util.Calendar

import spire.math.{Real, SafeLong}

/**
  * Created by dennis on 18/8/16.
  */
case class Price(date: Calendar,
                 open: Real,
                 high: Real,
                 low: Real,
                 close: Real,
                 volume: SafeLong,
                 adjusted: Real)
