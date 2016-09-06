package util

/**
  * Created by dennis on 5/9/16.
  */
object Transformer {
  /**
    * Convert List of Option to Option of List
    * @param l list to convert
    * @tparam T type of what the options hold
    * @return Option of List
    */
  def sequence[T](l: List[Option[T]]): Option[List[T]] = {
    if (l.contains(None)) None else Some(l.flatten)
  }
}
