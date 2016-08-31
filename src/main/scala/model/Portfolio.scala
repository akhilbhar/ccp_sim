package model

case class Portfolio(positions: List[Position]) {
  def addPosition(position: Position): Portfolio = {
    Portfolio(position +: positions)
  }

  def removePosition(position: Position): Portfolio = {
    Portfolio(positions.filterNot(_.equals(position)))
  }
}

sealed trait PortfolioError

object PortfolioError {

  case class InvalidPosition(position: Position)

}
