package util

import breeze.linalg.{*, DenseMatrix, DenseVector, cholesky}
import breeze.stats._
import spire.implicits._
import spire.math._

/**
  * Created by dennis on 1/9/16.
  */
object Math {
  def mean[T: Numeric](xs: Vector[T]): T = xs.qsum / xs.length

  def stddev[T: Numeric](xs: Vector[T]): T = {
    val mu = mean(xs)

    val a = xs.map(x => x - mu).foldLeft(Numeric[T].zero)((acc, x) => acc + x * x)

    sqrt(a / xs.length)
  }

  /**
    * Creates a Vector with the returns.
    * @param data data on which to compute the returns.
    * @return returns
    */
  def change[T: Numeric](data: Vector[T]): Vector[T] = {
    data
      .sliding(2)
      .map {
        case Seq(a, b, _ *) => (a - b) / b
      }
      .toVector
  }

  /**
    * Computes the mean of the returns of Vector data
    * @param data the data
    * @return the mean of the changes in data
    */
  def meanOfChange[T: Numeric](data: Vector[T]): T = mean(change(data))

  /**
    * Computes the standard deviation of the change in data.
    * @param data the data
    * @return the standard deviation of the changes in data.
    */
  def volatilityOfChange[T: Numeric](data: Vector[T]): T = stddev(change(data))

  implicit object opMulInnerReal
    extends breeze.linalg.operators.OpMulInner.Impl2[DenseVector[Real], DenseVector[Real], Real] {
    override def apply(xs: DenseVector[Real], ys: DenseVector[Real]): Real = {
      val foo = for {
        x <- xs.activeValuesIterator
        y <- ys.activeValuesIterator
      } yield x * y

      foo.foldLeft(Real.zero)(_ + _)
    }
  }

  implicit object opMulMatrixReal
  extends breeze.linalg.operators.OpMulMatrix.Impl2[DenseMatrix[Real],
  DenseVector[Real],
  DenseVector[Real]] {
    override def apply(m: DenseMatrix[Real], v: DenseVector[Real]): DenseVector[Real] = {
      for (v1 <- m(*, ::)) yield v1 dot v
    }
  }

  implicit object covmatReal extends covmat.Impl[DenseMatrix[Real], DenseMatrix[Real]] {
    override def apply(v: DenseMatrix[Real]): DenseMatrix[Real] = {
      def cov(x: DenseVector[Real], y: DenseVector[Real]): Real = {
        val meanX = mean(x.toScalaVector())
        val meanY = mean(y.toScalaVector())

        val a = (for {
          (xi, yi) <- x.toArray zip y.toArray
        } yield (xi - meanX) * (yi - meanY)).foldLeft(Real.zero)(_ + _)

        a / (x.length - 1)
      }

      val foo = new DenseMatrix[Real](v.cols, v.cols, (for {
        i <- 0 until v.cols
        j <- 0 until v.cols
      } yield {
        cov(v(::, i), v(::, j))
      }).toArray)

      println(foo)

      foo
    }
  }

  implicit object choleskyReal extends cholesky.Impl[DenseMatrix[Real], DenseMatrix[Real]] {
    override def apply(v: DenseMatrix[Real]): DenseMatrix[Real] = {
      val doubleMatrix = new DenseMatrix(v.rows, v.cols, v.toArray.map(_.toDouble))

      val doubleCholesky = cholesky(doubleMatrix)

      new DenseMatrix(doubleCholesky.rows,
                      doubleCholesky.cols,
                      doubleCholesky.toArray.map(Real(_)))
    }
  }

//  def *[T: Numeric](m: Vector[Vector[T]], v: Vector[T]): Vector[T] = {
//    assert(v.length == m.length)
//
//    def vectMult(xs: Vector[T], ys: Vector[T]): T = {
//      (for ((x, y) <- xs zip ys) yield x * y).qsum
//    }
//
//    for (v1 <- m) yield vectMult()
//  }

//  // make sure this is imported anywhere you want to use BigDecimal operators
//  implicit object fieldBigDecimal extends Field[BigDecimal] {
//    override def zero = BigDecimal(0L)
//    override def one = BigDecimal(1L)
//    override def ==(a: BigDecimal, b: BigDecimal) = a == b
//    override def !=(a: BigDecimal, b: BigDecimal) = a != b
//    override def +(a: BigDecimal, b: BigDecimal) = a + b
//
//    override def /(a: BigDecimal, b: BigDecimal): BigDecimal = a / b
//
//    override def pow(a: BigDecimal, b: BigDecimal): BigDecimal = pow(a.toDouble, b.toDouble)
//
//    override def -(a: BigDecimal, b: BigDecimal): BigDecimal = a - b
//
//    override def %(a: BigDecimal, b: BigDecimal): BigDecimal = a % b
//
//    override implicit val normImpl: _root_.breeze.linalg.norm.Impl[BigDecimal, Double] =
//      new norm.Impl[BigDecimal, Double] {
//        def apply(v: BigDecimal) = v.toDouble
//      }
//
//    override def *(a: BigDecimal, b: BigDecimal): BigDecimal = a * b
//  }
//
//  implicit object implSqrtBigDecimal extends sqrt.Impl[BigDecimal, BigDecimal] {
//    override def apply(v: BigDecimal): BigDecimal = {
//      assert(v >= 0)
//
//      def isGoodEnough(guess: BigDecimal, x: BigDecimal, precision: BigDecimal): Boolean = {
//        (guess * guess - x).abs / x < precision
//      }
//
//      def improve(guess: BigDecimal, x: BigDecimal): BigDecimal = {
//        (guess + x / guess) / BigDecimal(2)
//      }
//
//      def sqrtIter(guess: BigDecimal, x: BigDecimal, precision: BigDecimal): BigDecimal = {
//        if (isGoodEnough(guess, x, precision)) guess
//        else sqrtIter(improve(guess, x), x, precision)
//      }
//
//      val precision = BigDecimal(1) / pow(BigDecimal(10), BigDecimal(150))
//
//      sqrtIter(sqrt(v.toDouble), v, precision)
//    }
//  }

//  implicit object implExpBigDecimal extends exp.Impl[BigDecimal, BigDecimal] {
//    override def apply(v: BigDecimal): BigDecimal = exp(v.doubleValue())
//  }
//
//  implicit object implLogBigDecimal extends log.Impl[BigDecimal, BigDecimal] {
//    override def apply(v: BigDecimal): BigDecimal = {
//      assert(v > 0)
//    }
//  }
//
//  implicit object implMeanBigDecimal extends mean.Impl[Vector[BigDecimal], BigDecimal] {
//    override def apply(v: Vector[BigDecimal]): BigDecimal = {
//      v.sum / v.length
//    }
//  }
//
//  implicit object implStddevBigDecimal extends stddev.Impl[Vector[BigDecimal], BigDecimal] {
//    override def apply(v: Vector[BigDecimal]): BigDecimal = {
//      val mu = mean(v)
//
//      val a: BigDecimal = v.map(e => e - mu).map(pow(_, BigDecimal(2))).sum
//
//      sqrt(a / BigDecimal(v.length))
//    }
//  }
//
//  implicit object implCovmatBigDecimal
//      extends covmat.Impl[DenseMatrix[BigDecimal], DenseMatrix[BigDecimal]] {
//    override def apply(v: DenseMatrix[BigDecimal]): DenseMatrix[BigDecimal] = {
//      def cov(x: DenseVector[BigDecimal], y: DenseVector[BigDecimal]): BigDecimal = {
//        val meanX = mean(x)
//        val meanY = mean(y)
//
//        val a = (for {
//          xi <- x.activeValuesIterator
//          yi <- y.activeValuesIterator
//        } yield (xi - meanX) * (yi - meanY)).sum
//
//        a / (x.length - BigDecimal(1))
//      }
//
//      new DenseMatrix[BigDecimal](v.cols, v.cols, (for {
//        i <- 0 until (v.cols - 1)
//        j <- (i + 1) until v.cols
//      } yield {
//        cov(v(i), v(j))
//      }).toArray)
//    }
//  }
//
//  implicit object implCholeskyBigDecimal
//      extends cholesky.Impl[DenseMatrix[BigDecimal], DenseMatrix[BigDecimal]] {
//    override def apply(v: DenseMatrix[BigDecimal]): DenseMatrix[BigDecimal] = {
//      val doubleMatrix = new DenseMatrix(v.rows, v.cols, v.toArray.map(_.toDouble))
//
//      val doubleCholesky = cholesky(doubleMatrix)
//
//      new DenseMatrix(doubleCholesky.rows,
//                      doubleCholesky.cols,
//                      doubleCholesky.toArray.map(BigDecimal(_)))
//    }
//  }
}
