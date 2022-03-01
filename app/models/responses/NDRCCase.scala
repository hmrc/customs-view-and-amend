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

import akka.http.scaladsl.model.DateTime
import helpers.DateFormatters
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import utils.DateTimeUtil

case class NDRCCase(
                     NDRCDetail: NDRCDetail,
                     NDRCAmounts: NDRCAmounts
                   ) extends DateFormatters {

  private def transformCaseStatus: ClaimStatus = {
    NDRCDetail.caseStatus match {
      case "In Progress" => InProgress
      case "Pending" => Pending
      case "Closed" => Closed
      case e => throw new RuntimeException(s"Unknown case status: $e")
    }
  }

  def toClaimDetail(lrn: Option[String]): ClaimDetail = {
    ClaimDetail(
      NDRCDetail.CDFPayCaseNumber,
      NDRC,
      NDRCDetail.declarationID,
      NDRCDetail.MRNDetails.getOrElse(Seq.empty),
      NDRCDetail.entryDetails.getOrElse(Seq.empty),
      lrn,
      NDRCDetail.claimantEORI,
      transformCaseStatus,
      Some(NDRCDetail.claimType),
      DateTimeUtil.toDateTime(NDRCDetail.claimStartDate),
      NDRCDetail.closedDate.map(DateTimeUtil.toDateTime),
      NDRCAmounts.totalClaimAmount,
      NDRCDetail.claimantName,
      NDRCDetail.claimantEmailAddress
    )
  }
}

object NDRCCase {
  implicit val reads: Reads[NDRCCase] = {
    (JsPath.read[NDRCDetail] and JsPath.read[NDRCAmounts])(NDRCCase.apply _)
  }

  implicit val writes: Writes[NDRCCase] = {
    (JsPath.write[NDRCDetail] and JsPath.write[NDRCAmounts])(unlift(NDRCCase.unapply))
  }
}