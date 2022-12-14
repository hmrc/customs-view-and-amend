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
import utils.DateTimeUtil

case class SCTYCase(
                     CDFPayCaseNumber: String,
                     declarationID: String,
                     reasonForSecurity: String,
                     procedureCode: String,
                     caseStatus: String,
                     caseSubStatus: Option[String],
                     caseType: Option[CaseType],
                     goods: Option[Seq[Goods]],
                     declarantEORI: String,
                     importerEORI: String,
                     claimantEORI: Option[String],
                     totalCustomsClaimAmount: Option[String],
                     totalVATClaimAmount: Option[String],
                     totalClaimAmount: Option[String],
                     totalReimbursementAmount: Option[String],
                     claimStartDate: String,
                     claimantName: Option[String],
                     claimantEmailAddress: Option[String],
                     closedDate: Option[String],
                     reimbursement: Option[Seq[Reimbursement]]
                   ) {

  private def transformCaseStatus: ClaimStatus = {
    caseStatus match {
      case "In Progress" => InProgress
      case "Pending" => Pending
      case "Closed" => Closed
      case e => throw new RuntimeException(s"Unknown case status: $e")
    }
  }

  private def getSecurityGoodsDescription: Option[String] = {
    goods
      .map(_
        .flatMap(_.goodsDescription)
        .filter(_.trim.nonEmpty)
        .mkString(", ")
      )
  }


  def toClaimDetail(lrn: Option[String]): ClaimDetail = {
    ClaimDetail(
      CDFPayCaseNumber,
      SCTY,
      declarationID,
      Seq.empty,
      Seq.empty,
      lrn,
      claimantEORI,
      transformCaseStatus,
      caseSubStatus,
      None,
      None,
      DateTimeUtil.toDateTime(claimStartDate),
      closedDate.map(DateTimeUtil.toDateTime),
      totalClaimAmount,
      claimantName,
      claimantEmailAddress,
      Some(reasonForSecurity),
      getSecurityGoodsDescription
    )
  }

}

object SCTYCase {
  implicit val format: OFormat[SCTYCase] = Json.format[SCTYCase]
}