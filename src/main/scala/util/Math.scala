package util

import breeze.stats._

/**
  * Created by dennis on 1/9/16.
  */
object Math {
  /**
    * Creates a Vector with the returns.
    * @param data data on which to compute the returns.
    * @return returns
    */
   def change(data: Vector[Double]): Vector[Double] = {
    val a = data
      .sliding(2)
      .map {
        case Seq(a, b, _ *) => (a - b) / b
      }
      .toVector

     println(a)

     a
  }

  /**
    * Computes the mean of the returns of Vector data
    * @param data the data
    * @return the mean of the changes in data
    */
   def meanOfChange(data: Vector[Double]): Double = mean(change(data))

  /**
    * Computes the standard deviation of the change in data.
    * @param data the data
    * @return the standard deviation of the changes in data.
    */
   def volatilityOfChange(data: Vector[Double]): Double = stddev(change(data))
}
