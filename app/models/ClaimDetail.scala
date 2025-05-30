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

package models

import helpers.DateFormatters
import models.responses.{ClaimType, EntryDetail, ProcedureDetail}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class ClaimDetail(
  caseNumber: String,
  serviceType: ServiceType,
  declarationId: Option[String],
  mrn: Seq[ProcedureDetail],
  entryNumbers: Seq[EntryDetail],
  lrn: Option[String],
  claimantsEori: Option[String],
  declarantEori: String,
  importerEori: Option[String],
  claimStatus: ClaimStatus,
  caseSubStatus: Option[String],
  claimType: Option[ClaimType],
  caseType: Option[CaseType],
  claimStartDate: Option[LocalDate],
  claimClosedDate: Option[LocalDate],
  totalClaimAmount: Option[String],
  claimantsName: Option[String],
  claimantsEmail: Option[String],
  reasonForSecurity: Option[String] = None,
  securityGoodsDescription: Option[String] = None,
  mrnDetails: Option[Seq[ProcedureDetail]] = None
) extends DateFormatters {

  def formattedStartDate()(implicit messages: Messages): Option[String] =
    claimStartDate.map(dateAsDayMonthAndYear)

  def formattedClosedDate()(implicit messages: Messages): Option[String] =
    claimClosedDate.map(dateAsDayMonthAndYear)

  def multipleDeclarations: Boolean =
    mrn.size > 1 || entryNumbers.size > 1

  def isPending: Boolean = claimStatus match {
    case Pending => true
    case _       => false
  }

  def isConnectedTo(eori: String): Boolean =
    val e = eori.toUpperCase().trim()
    declarantEori == e || importerEori.contains(e) || claimantsEori.contains(e)
}

object ClaimDetail {
  implicit val format: OFormat[ClaimDetail] = Json.format[ClaimDetail]
}
