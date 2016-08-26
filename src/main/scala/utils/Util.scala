package utils

/**
  * Created by dennis on 25/8/16.
  */
object Util {
  def sequence[T](lo: List[Option[T]]): Option[List[T]] = {
    if (lo.contains(None)) None else Some(lo.flatten)
  }
}
