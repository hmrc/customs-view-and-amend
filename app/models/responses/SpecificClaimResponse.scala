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

package models.responses

import models._
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class SpecificClaimResponse(
                             caseStatus: String,
                             CDFPayCaseNumber: String,
                             claimantEORI: Option[String]
                           ) {

  private def transformCaseStatus: ClaimStatus = {
    caseStatus match {
      case "In Progress" => InProgress
      case "Pending" => Pending
      case "Closed" => Closed
      case e => throw new RuntimeException(s"Unknown case status: $e")
    }
  }

  //TODO Implement values once API specs produced
  def toClaimDetail(claimType: ClaimType) = ClaimDetail(
    CDFPayCaseNumber,
    Seq("AWAITING API SPEC"),
    claimantEORI,
    transformCaseStatus,
    claimType,
    LocalDate.of(9999,1,1),
    123456789,
    "AWAITING API SPEC",
    "AWAITING API SPEC"
  )
}


object SpecificClaimResponse {
  implicit val format: OFormat[SpecificClaimResponse] = Json.format[SpecificClaimResponse]
}


