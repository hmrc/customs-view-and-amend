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

import models.responses.{C285, ClaimType, `C&E1179`}
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
      case FileSelection.AdditionalSupportingDocuments => "Additional Supporting Documentation"
      case FileSelection.AirwayBill => "Air Waybill"
      case FileSelection.AirworthinessCertificates => "Airworthiness certificates"
      case FileSelection.BillOfLading => "Bill of Lading"
      case FileSelection.CalculationWorksheet => "Calculation worksheet"
      case FileSelection.CommercialInvoice => "Commercial Invoice"
      case FileSelection.CorrespondenceTrader => "Correspondence Trader"
      case FileSelection.ProofOfFaultyUnwantedGoods => "Documentary proof that the goods are faulty or not what you ordered"
      case FileSelection.ImportAndExportDeclaration => "Import and Export Declaration"
      case FileSelection.PackingList => "Packing List"
      case FileSelection.PreferenceCertificate => "Preference certificate"
      case FileSelection.LetterOfAuthority => "Proof of Authority (to be repaid)"
      case FileSelection.ProofOfExportOrDestruction => "Proof of export or destruction"
      case FileSelection.ProofOfOrigin => "Proof of origin"
      case FileSelection.SubstituteEntry => "Substitute Entry"
      case FileSelection.TechnicalSpecifications => "Technical specifications"
    }
}

object FileSelection extends Enumerable.Implicits {

  val `C&E1179Values`: Seq[FileSelection] = Seq(
    CommercialInvoice,
    ImportAndExportDeclaration,
    LetterOfAuthority,
    ProofOfFaultyUnwantedGoods,
    CorrespondenceTrader,
    CalculationWorksheet,
    ProofOfExportOrDestruction,
    AdditionalSupportingDocuments
  )
  val C285Values: Seq[FileSelection] = Seq(
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
    CorrespondenceTrader,
    AdditionalSupportingDocuments
  )

  val c285Enumerable: Enumerable[FileSelection] =
    Enumerable(C285Values.map(v => v.toString -> v): _*)

  val `C&E1179Enumerable`: Enumerable[FileSelection] =
    Enumerable(`C&E1179Values`.map(v => v.toString -> v): _*)

  case object TechnicalSpecifications extends WithName("technical-specifications") with FileSelection

  case object ProofOfOrigin extends WithName("proof-of-origin") with FileSelection

  case object PreferenceCertificate extends WithName("preference-certificate") with FileSelection

  case object AirworthinessCertificates extends WithName("airworthiness-certificates") with FileSelection

  case object AirwayBill extends WithName("air-waybill") with FileSelection

  case object BillOfLading extends WithName("bill-of-lading") with FileSelection

  case object PackingList extends WithName("packing-list") with FileSelection

  case object SubstituteEntry extends WithName("substitute-entry") with FileSelection

  case object CommercialInvoice extends WithName("commercial-invoice") with FileSelection

  case object ImportAndExportDeclaration extends WithName("import-export-declaration") with FileSelection

  case object LetterOfAuthority extends WithName("letter-of-authority") with FileSelection

  case object ProofOfFaultyUnwantedGoods extends WithName("faulty-not-ordered") with FileSelection

  case object CorrespondenceTrader extends WithName("correspondence") with FileSelection

  case object CalculationWorksheet extends WithName("calculation-worksheet") with FileSelection

  case object AdditionalSupportingDocuments extends WithName("additional-supporting-documents") with FileSelection

  case object ProofOfExportOrDestruction extends WithName("proof-of-export-or-destruction") with FileSelection

  implicit val format: Format[FileSelection] = new Format[FileSelection] {
    override def writes(o: FileSelection): JsValue =
      o match {
        case AdditionalSupportingDocuments => JsString("Additional supporting documents")
        case AirwayBill => JsString("Air waybill")
        case AirworthinessCertificates => JsString("Airworthiness certificates")
        case BillOfLading => JsString("Bill of lading")
        case CalculationWorksheet => JsString("Calculation worksheet")
        case CommercialInvoice => JsString("Commercial invoice")
        case CorrespondenceTrader => JsString("Correspondence between trader and agent")
        case ProofOfFaultyUnwantedGoods => JsString("Documentary proof that the goods are faulty or not what you ordered")
        case ImportAndExportDeclaration => JsString("Import and export declaration")
        case PackingList => JsString("Packing list")
        case PreferenceCertificate => JsString("Preference certificate")
        case LetterOfAuthority => JsString("Letter of authority")
        case ProofOfExportOrDestruction => JsString("Proof of export or destruction")
        case ProofOfOrigin => JsString("Proof of origin")
        case SubstituteEntry => JsString("Substitute entry")
        case TechnicalSpecifications => JsString("Technical specifications")
      }

    override def reads(json: JsValue): JsResult[FileSelection] =
      json match {
        case JsString("Additional supporting documents") => JsSuccess(AdditionalSupportingDocuments)
        case JsString("Air waybill") => JsSuccess(AirwayBill)
        case JsString("Airworthiness certificates") => JsSuccess(AirworthinessCertificates)
        case JsString("Bill of lading") => JsSuccess(BillOfLading)
        case JsString("Calculation worksheet") => JsSuccess(CalculationWorksheet)
        case JsString("Commercial invoice") => JsSuccess(CommercialInvoice)
        case JsString("Correspondence between trader and agent") => JsSuccess(CorrespondenceTrader)
        case JsString("Documentary proof that the goods are faulty or not what you ordered") => JsSuccess(ProofOfFaultyUnwantedGoods)
        case JsString("Import and export declaration") => JsSuccess(ImportAndExportDeclaration)
        case JsString("Packing list") => JsSuccess(PackingList)
        case JsString("Preference certificate") => JsSuccess(PreferenceCertificate)
        case JsString("Letter of authority") => JsSuccess(LetterOfAuthority)
        case JsString("Proof of export or destruction") => JsSuccess(ProofOfExportOrDestruction)
        case JsString("Proof of origin") => JsSuccess(ProofOfOrigin)
        case JsString("Substitute entry") => JsSuccess(SubstituteEntry)
        case JsString("Technical specifications") => JsSuccess(TechnicalSpecifications)
        case e => JsError(s"Unknown document type: $e")
      }
  }

  def options(form: Form[_], claimType: ClaimType)(implicit messages: Messages): Seq[RadioItem] = {
    val values = claimType match {
      case C285 => C285Values
      case `C&E1179` => `C&E1179Values`
    }

    values.map { fileType =>
      RadioItem(
        value = Some(fileType.toString),
        content = Text(messages(s"file.selection.${fileType.toString}")),
        checked = form("value").value.contains(fileType.toString)
      )
    }
  }
}


