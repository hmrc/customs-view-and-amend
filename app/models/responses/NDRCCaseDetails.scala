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

import models.*
import play.api.libs.json.{Json, OFormat}
import utils.DateTimeUtil.toDateTime

case class NDRCCaseDetails(
  CDFPayCaseNumber: String,
  declarationID: String,
  claimStartDate: String,
  closedDate: Option[String],
  caseStatus: String,
  caseSubStatus: Option[String],
  declarantEORI: String,
  importerEORI: String,
  claimantEORI: Option[String],
  totalCustomsClaimAmount: Option[String],
  totalVATClaimAmount: Option[String],
  totalExciseClaimAmount: Option[String],
  declarantReferenceNumber: Option[String],
  basisOfClaim: Option[String]
) {

  def toClaim: Claim = {
    val startDate = toDateTime(claimStartDate)
    caseStatus match {
      case "In Progress" =>
        InProgressClaim(declarationID, CDFPayCaseNumber, NDRC, declarantReferenceNumber, Some(startDate))
      case "Pending"     =>
        PendingClaim(
          declarationID,
          CDFPayCaseNumber,
          NDRC,
          declarantReferenceNumber,
          Some(startDate),
          Some(startDate.plusDays(30)),
          None
        )
      case "Closed"      =>
        ClosedClaim(
          declarationID,
          CDFPayCaseNumber,
          NDRC,
          declarantReferenceNumber,
          Some(startDate),
          closedDate.map(toDateTime),
          caseSubStatus.getOrElse("")
        )
      case e             => throw new RuntimeException(s"Unknown Case Status: $e")
    }
  }
}

object NDRCCaseDetails {
  implicit val format: OFormat[NDRCCaseDetails] = Json.format[NDRCCaseDetails]
}
