package model

case class Portfolio(positions: List[Position]) {
  def addPosition(position: Position): Portfolio = {
    Portfolio(position +: positions)
//    positions.find(p =>
//      position.instrument.equals(p.instrument) && position.counterParty.equals(p.counterParty))
//
//    positions collectFirst {
//      case p
//          if position.instrument.equals(p.instrument) && position.counterParty.equals(
//            p.counterParty) => {
//        val newVolumeLong = aggregateVolume(p, position)
//
//        if (newVolumeLong._1 != 0) {
//          Portfolio(
//            (positions - p) + Position(p.instrument,
//                                       newVolumeLong._1,
//                                       p.counterParty,
//                                       newVolumeLong._2))
//        } else {
//          None
//        }
//      }
//    } match {
//      case Some(p) => Portfolio()
//      case None =>
//    }
  }

  def removePosition(position: Position): Portfolio = {
    Portfolio(positions.filterNot(_.equals(position)))
  }

//  private def aggregateVolume(oldPosition: Position, newPosition: Position): (Int, Boolean) = {
//    val newVolume = normalizeVolume(oldPosition) + normalizeVolume(newPosition)
//
//    (Math.abs(newVolume), newVolume >= 0)
//  }
//
//  private def normalizeVolume(position: Position): Int = {
//    if (!position.long) -position.volume else position.volume
//  }
}

sealed trait PortfolioError

object PortfolioError {
  case class InvalidPosition(position: Position)
}
