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

sealed trait FileSelection {
  def message(implicit messages: Messages): String =
    messages(s"file.selection.${this.toString}")

  def toDec64FileType: String =
    this match {
      case FileSelection.AdditionalSupportingDocuments => "Other"
      case FileSelection.AirwayBill                    => "Air Waybill"
      case FileSelection.BillOfLading                  => "Bill of Lading"
      case FileSelection.CalculationWorksheet          => "Other"
      case FileSelection.CommercialInvoice             => "Commercial Invoice"
      case FileSelection.ImportAndExportDeclaration    => "Import and Export Declaration"
      case FileSelection.PackingList                   => "Packing List"
      case FileSelection.ProofOfAuthority              => "Proof of Authority (to be repaid)"
      case FileSelection.SubstituteEntry               => "Substitute Entry"
    }
}

object FileSelection extends Enumerable.Implicits {

  val values: Seq[FileSelection] = Seq(
    CommercialInvoice,
    ImportAndExportDeclaration,
    PackingList,
    AirwayBill,
    BillOfLading,
    SubstituteEntry,
    ProofOfAuthority,
    CalculationWorksheet,
    AdditionalSupportingDocuments
  )

  val enumerable: Enumerable[FileSelection] =
    Enumerable(values.map(v => v.toString -> v): _*)

  case object AirwayBill extends WithName("air-waybill") with FileSelection

  case object BillOfLading extends WithName("bill-of-lading") with FileSelection

  case object PackingList extends WithName("packing-list") with FileSelection

  case object SubstituteEntry extends WithName("substitute-entry") with FileSelection

  case object CommercialInvoice extends WithName("commercial-invoice") with FileSelection

  case object ImportAndExportDeclaration extends WithName("import-export-declaration") with FileSelection

  case object ProofOfAuthority extends WithName("proof-of-authority") with FileSelection

  case object CalculationWorksheet extends WithName("calculation-worksheet") with FileSelection

  case object AdditionalSupportingDocuments extends WithName("other-supporting-documents") with FileSelection

  implicit val format: Format[FileSelection] = new Format[FileSelection] {
    override def writes(o: FileSelection): JsValue =
      o match {
        case AdditionalSupportingDocuments => JsString("Additional supporting documents")
        case AirwayBill                    => JsString("Air waybill")
        case BillOfLading                  => JsString("Bill of lading")
        case CalculationWorksheet          => JsString("Calculation worksheet")
        case CommercialInvoice             => JsString("Commercial invoice")
        case ImportAndExportDeclaration    => JsString("Import and export declaration")
        case PackingList                   => JsString("Packing list")
        case ProofOfAuthority              => JsString("Letter of authority")
        case SubstituteEntry               => JsString("Substitute entry")
      }

    override def reads(json: JsValue): JsResult[FileSelection] =
      json match {
        case JsString("Additional supporting documents") => JsSuccess(AdditionalSupportingDocuments)
        case JsString("Air waybill")                     => JsSuccess(AirwayBill)
        case JsString("Bill of lading")                  => JsSuccess(BillOfLading)
        case JsString("Calculation worksheet")           => JsSuccess(CalculationWorksheet)
        case JsString("Commercial invoice")              => JsSuccess(CommercialInvoice)
        case JsString("Import and export declaration")   => JsSuccess(ImportAndExportDeclaration)
        case JsString("Packing list")                    => JsSuccess(PackingList)
        case JsString("Letter of authority")             => JsSuccess(ProofOfAuthority)
        case JsString("Substitute entry")                => JsSuccess(SubstituteEntry)
        case e                                           => JsError(s"Unknown document type: $e")
      }
  }

  def options(form: Form[_])(implicit messages: Messages): Seq[RadioItem] =
    values.map { fileType =>
      RadioItem(
        value = Some(fileType.toString),
        content = Text(messages(s"file.selection.${fileType.toString}")),
        checked = form("value").value.contains(fileType.toString)
      )
    }
}
