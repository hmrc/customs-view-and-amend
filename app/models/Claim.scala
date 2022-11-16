/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import helpers.{DateFormatters, ServiceTypeFormatters}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

sealed trait Claim extends DateFormatters with ServiceTypeFormatters {
  val caseNumber: String
  val declarationId: String
  val serviceType: ServiceType
  val claimStartDate: LocalDate
  val lrn: Option[String]
  val claimStatus: ClaimStatus
  def url(searched: Boolean): String = claimStatus match {
    case InProgress => controllers.routes.ClaimDetailController.inProgressClaimDetail(caseNumber, serviceType, searched).url
    case Closed => controllers.routes.ClaimDetailController.closedClaimDetail(caseNumber, serviceType, searched).url
    case Pending => controllers.routes.ClaimDetailController.pendingClaimDetail(caseNumber, serviceType, searched).url
  }
  def formattedStartDate()(implicit messages: Messages): String = dateAsDayMonthAndYear(claimStartDate)
  def formattedServiceType()(implicit messages: Messages): String = serviceTypeAsMessage(serviceType)
}

sealed trait SubStatus {
  val subStatus: String
}
 
object Claim {
  implicit val format: OFormat[Claim] = Json.format[Claim]
}
case class InProgressClaim(declarationId: String, caseNumber: String, serviceType: ServiceType, lrn: Option[String], claimStartDate: LocalDate) extends Claim {
  override val claimStatus = InProgress
}

object InProgressClaim {
  implicit val format: OFormat[InProgressClaim] = Json.format[InProgressClaim]
}

case class PendingClaim(declarationId: String, caseNumber: String, serviceType: ServiceType, lrn: Option[String], claimStartDate: LocalDate, respondByDate: LocalDate) extends Claim {
  //TODO: Removed the respond by date until secure messaging timestamp available
  //def formattedRespondByDate()(implicit messages: Messages): String = dateAsDayMonthAndYear(respondByDate)
  override val claimStatus = Pending
}

object PendingClaim {
  implicit val format: OFormat[PendingClaim] = Json.format[PendingClaim]
}

case class ClosedClaim(declarationId: String, caseNumber: String, serviceType: ServiceType, lrn: Option[String], claimStartDate: LocalDate, removalDate: LocalDate, subStatus: String) extends Claim with SubStatus {
  override val claimStatus = Closed
  def formattedRemovalDate()(implicit messages: Messages): String = dateAsDayMonthAndYear(removalDate)

}

object ClosedClaim {
  implicit val format: OFormat[ClosedClaim] = Json.format[ClosedClaim]
}