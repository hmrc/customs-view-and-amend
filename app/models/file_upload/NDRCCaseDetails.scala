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

package models.file_upload

import java.time.LocalDate

import models._
import play.api.libs.json.{Json, OFormat}
import utils.DateTimeUtil
import utils.DateTimeUtil.toDateTime

case class NDRCCaseDetails(CDFPayCaseNumber: String,
                           declarationID: Option[String],
                           claimStartDate: String,
                           closedDate: Option[String],
                           caseStatus: String,
                           declarantEORI: String,
                           importerEORI: String,
                           claimantEORI: Option[String],
                           totalCustomsClaimAmount: Option[String],
                           totalVATClaimAmount: Option[String],
                           totalExciseClaimAmount: Option[String],
                           declarantReferenceNumber: Option[String],
                           basisOfClaim: Option[String]) {

  //TODO: dates need to be added from response when provided

  def toNdrcClaim: Claim =
    caseStatus match {
      case "In Progress" => InProgressClaim(CDFPayCaseNumber, C285, toDateTime(claimStartDate))
      case "Pending" => PendingClaim(CDFPayCaseNumber, C285, toDateTime(claimStartDate), LocalDate.of(9999, 1, 1))
      case "Closed" => ClosedClaim(CDFPayCaseNumber, C285, toDateTime(claimStartDate), toDateTime(closedDate.getOrElse("")))
      case e => throw new RuntimeException(s"Unknown Case Status: $e")
    }
}

object NDRCCaseDetails {
  implicit val format: OFormat[NDRCCaseDetails] = Json.format[NDRCCaseDetails]
}
