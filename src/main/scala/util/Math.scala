package util

import breeze.numerics.log
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
    data
      .sliding(2)
      .map {
        case Seq(a, b, _ *) => (a - b) / b
      }
      .toVector
  }

  def logChange(data: Vector[Double]): Vector[Double] = {
    data
      .sliding(2)
      .map {
        case Seq(a, b, _ *) => log(a / b)
      }
      .toVector
  }

  /**
    * Computes the mean of the returns of Vector data
    * @param data the data
    * @return the mean of the changes in data
    */
  def meanOfChange(data: Vector[Double]): Double = {
    mean(change(data))
  }

  /**
    * Computes the standard deviation of the change in data.
    * @param data the data
    * @return the standard deviation of the changes in data.
    */
  def logVolatilityOfChange(data: Vector[Double]): Double = {
    stddev(logChange(data))
  }

  def volatilityOfChange(data: Vector[Double]): Double = {
    stddev(change(data))
  }

  def sumList[T: Numeric](list: List[Option[T]]): Option[T] = {
    def sumList_(list: List[Option[T]], acc: T): Option[T] = {
      list match {
        case x :: xs =>
          x match {
            case Some(v) => sumList_(xs, implicitly[Numeric[T]].plus(acc, v))
            case None => None
          }
        case Nil => Some(acc)
      }
    }

    sumList_(list, implicitly[Numeric[T]].zero)
  }
}
