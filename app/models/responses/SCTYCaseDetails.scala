/*
 * Copyright 2023 HM Revenue & Customs
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

package models.responses

import models._
import play.api.libs.json.{Json, OFormat}
import utils.DateTimeUtil.toDateTime
import java.time.LocalDate

case class SCTYCaseDetails(
  CDFPayCaseNumber: String,
  declarationID: String,
  claimStartDate: Option[String],
  closedDate: Option[String],
  reasonForSecurity: String,
  caseStatus: String,
  caseSubStatus: Option[String],
  declarantEORI: String,
  importerEORI: Option[String],
  claimantEORI: Option[String],
  totalCustomsClaimAmount: Option[String],
  totalVATClaimAmount: Option[String],
  declarantReferenceNumber: Option[String]
) {

  def toClaim: Claim = {
    val startDate: Option[LocalDate] =
      claimStartDate.map(toDateTime)

    caseStatus match {
      case "In Progress" =>
        InProgressClaim(declarationID, CDFPayCaseNumber, SCTY, declarantReferenceNumber, startDate)

      case "Pending" =>
        PendingClaim(
          declarationID,
          CDFPayCaseNumber,
          SCTY,
          declarantReferenceNumber,
          startDate,
          startDate.map(_.plusDays(30)),
          Some(reasonForSecurity)
        )

      case "Closed" =>
        ClosedClaim(
          declarationID,
          CDFPayCaseNumber,
          SCTY,
          declarantReferenceNumber,
          startDate,
          closedDate.map(toDateTime),
          caseSubStatus.getOrElse("")
        )
      case e        => throw new RuntimeException(s"Unknown Case Status: $e")
    }
  }
}

object SCTYCaseDetails {
  implicit val format: OFormat[SCTYCaseDetails] = Json.format[SCTYCaseDetails]
}
