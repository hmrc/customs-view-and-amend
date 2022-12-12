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

import helpers.DateFormatters
import models.responses.{ClaimType, EntryDetail, ProcedureDetail}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class ClaimDetail(caseNumber: String,
                       serviceType: ServiceType,
                       declarationId: String,
                       mrn: Seq[ProcedureDetail],
                       entryNumbers: Seq[EntryDetail],
                       lrn: Option[String],
                       claimantsEori: Option[String],
                       claimStatus: ClaimStatus,
                       caseSubStatus: Option[String],
                       claimType: Option[ClaimType],
                       claimStartDate: LocalDate,
                       claimClosedDate: Option[LocalDate],
                       totalClaimAmount: Option[String],
                       claimantsName: Option[String],
                       claimantsEmail: Option[String],
                       reasonForSecurity: Option[String] = None,
                       securityGoodsDescription: Option[String] = None
                      ) extends DateFormatters {

 def formattedStartDate()(implicit messages: Messages): String = {
   dateAsDayMonthAndYear(claimStartDate)
 }

  def formattedClosedDate()(implicit messages: Messages): Option[String] = {
    claimClosedDate.map(dateAsDayMonthAndYear)
  }

  def isEntryNumber: Boolean = {
    val entryNumberRegex = "^[0-9]{9}[A-Za-z][0-9]{8}".r
    entryNumberRegex.findFirstIn(declarationId).isDefined
  }

  def multipleDeclarations: Boolean = {
    mrn.size > 1 || entryNumbers.size > 1
  }

  def isPending: Boolean = claimStatus match {
    case Pending => true
    case _ => false
  }

  def backLink(): String =
    claimStatus match {
      case InProgress => controllers.routes.ClaimListController.showInProgressClaimList(None).url
      case Pending => controllers.routes.ClaimListController.showPendingClaimList(None).url
      case Closed => controllers.routes.ClaimListController.showClosedClaimList(None).url
    }
}

object ClaimDetail {
  implicit val format: OFormat[ClaimDetail] = Json.format[ClaimDetail]
}
