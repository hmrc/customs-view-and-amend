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

import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait C285FileSelection

object C285FileSelection extends Enumerable.Implicits {

  val values: Seq[C285FileSelection] = Seq(
    TechnicalSpecifications,
    ProofOfOrigin,
    PreferenceCertificate,
    AirworthinessCertificates,
    CommercialInvoice,
    ImportAndExportDeclaration,
    AirwayBill,
    CalculationWorksheet,
    BillOfLading,
    PackingList,
    SubstituteEntry,
    LetterOfAuthority,
    Correspondence,
    AdditionalSupportingDocuments
  )

  implicit val enumerable: Enumerable[C285FileSelection] =
    Enumerable(values.map(v => v.toString -> v): _*)

  case object TechnicalSpecifications extends WithName("technical-specifications") with C285FileSelection
  case object ProofOfOrigin extends WithName("proof-of-origin") with C285FileSelection
  case object PreferenceCertificate extends WithName("preference-certificate") with C285FileSelection
  case object AirworthinessCertificates extends WithName("airworthiness-certificates") with C285FileSelection
  case object CommercialInvoice extends WithName("commercial-invoice") with C285FileSelection
  case object ImportAndExportDeclaration extends WithName("import-export-declaration") with C285FileSelection
  case object AirwayBill extends WithName("air-waybill") with C285FileSelection
  case object CalculationWorksheet extends WithName("calculation-worksheet") with C285FileSelection
  case object BillOfLading extends WithName("bill-of-lading") with C285FileSelection
  case object PackingList extends WithName("packing-list") with C285FileSelection
  case object SubstituteEntry extends WithName("substitute-entry") with C285FileSelection
  case object LetterOfAuthority extends WithName("letter-of-authority") with C285FileSelection
  case object Correspondence extends WithName("correspondence") with C285FileSelection
  case object AdditionalSupportingDocuments extends WithName("additional-supporting-documents") with C285FileSelection

  implicit val format: Format[C285FileSelection] = new Format[C285FileSelection] {
    override def writes(o: C285FileSelection): JsValue =
      o match {
        case TechnicalSpecifications => JsString("Technical specifications")
        case ProofOfOrigin => JsString("Proof of origin")
        case PreferenceCertificate => JsString("Preference certificate")
        case AirworthinessCertificates => JsString("Airworthiness certificates")
        case CommercialInvoice => JsString("Commercial Invoice")
        case ImportAndExportDeclaration => JsString("Import and Export Declaration")
        case AirwayBill => JsString("Air Waybill")
        case CalculationWorksheet => JsString("Calculation worksheet")
        case BillOfLading => JsString("Bill of Lading")
        case PackingList => JsString("Packing List")
        case SubstituteEntry => JsString("Substitute Entry")
        case LetterOfAuthority => JsString("Proof of Authority (to be repaid)")
        case Correspondence => JsString("Correspondence between trader and agent")
        case AdditionalSupportingDocuments => JsString("Additional supporting documents")
      }

    override def reads(json: JsValue): JsResult[C285FileSelection] =
      json match {
        case JsString("Technical specifications") => JsSuccess(TechnicalSpecifications)
        case JsString("Proof of origin") => JsSuccess(ProofOfOrigin)
        case JsString("Preference certificate") => JsSuccess(PreferenceCertificate)
        case JsString("Airworthiness certificates") => JsSuccess(AirworthinessCertificates)
        case JsString("Commercial Invoice") => JsSuccess(CommercialInvoice)
        case JsString("Import and Export Declaration") => JsSuccess(ImportAndExportDeclaration)
        case JsString("Air Waybill") => JsSuccess(AirwayBill)
        case JsString("Calculation worksheet") => JsSuccess(CalculationWorksheet)
        case JsString("Bill of Lading") => JsSuccess(BillOfLading)
        case JsString("Packing List") => JsSuccess(PackingList)
        case JsString("Substitute Entry") => JsSuccess(SubstituteEntry)
        case JsString("Proof of Authority (to be repaid)") => JsSuccess(LetterOfAuthority)
        case JsString("Correspondence between trader and agent") => JsSuccess(Correspondence)
        case JsString("Additional supporting documents") => JsSuccess(AdditionalSupportingDocuments)
        case e => JsError(s"Unknown document type: $e")
      }
  }

  def options(form: Form[_])(implicit messages: Messages): Seq[RadioItem] = {
    values.map { fileType =>
      RadioItem(
        value = Some(fileType.toString),
        content = Text(messages(s"file.selection.c285.${fileType.toString}")),
        checked = form("value").value.contains(fileType.toString)
      )
    }
  }
}

