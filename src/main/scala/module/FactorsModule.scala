package module

import java.time.LocalDate

/**
  * Created by dennis on 18/8/16.
  */
trait FactorsModule {
  case class FactorsParameters(riskFreeRate: Double = 0,
                               timeHorizon: Int = 1000)

  protected def marketFactors(date: LocalDate)(
      implicit parameters: FactorsParameters)
}
