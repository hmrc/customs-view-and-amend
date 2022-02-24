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
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait CE1179FileSelection

object CE1179FileSelection extends Enumerable.Implicits {

  val values: Seq[CE1179FileSelection] = Seq(
    CommercialInvoice,
    ImportAndExportDeclaration,
    LetterOfAuthority,
    ProofOfFaultyUnwantedGoods,
    Correspondence,
    CalculationWorksheet,
    ProofOfExportOrDestruction,
    AdditionalSupportingDocuments
  )

  implicit val enumerable: Enumerable[CE1179FileSelection] =
    Enumerable(values.map(v => v.toString -> v): _*)

  case object CommercialInvoice extends WithName("commercial-invoice") with CE1179FileSelection
  case object ImportAndExportDeclaration extends WithName("import-export-declaration") with CE1179FileSelection
  case object LetterOfAuthority extends WithName("letter-of-authority") with CE1179FileSelection
  case object ProofOfFaultyUnwantedGoods extends WithName("faulty-not-ordered") with CE1179FileSelection
  case object Correspondence extends WithName("correspondence") with CE1179FileSelection
  case object CalculationWorksheet extends WithName("calculation-worksheet") with CE1179FileSelection
  case object AdditionalSupportingDocuments extends WithName("addition-supporting-documents") with CE1179FileSelection
  case object ProofOfExportOrDestruction extends WithName("proof-of-export-or-destruction") with CE1179FileSelection


  def fromString(fileType: String): CE1179FileSelection = {
    values.find(_.toString == fileType) match {
      case Some(value) => value
      case None => AdditionalSupportingDocuments
    }
  }

  implicit val format: Format[CE1179FileSelection] = new Format[CE1179FileSelection] {
    override def writes(o: CE1179FileSelection): JsValue =
      o match {
        case CommercialInvoice => JsString("Commercial Invoice")
        case ImportAndExportDeclaration => JsString("Import and Export Declaration")
        case LetterOfAuthority => JsString("Proof of Authority (to be repaid)")
        case ProofOfFaultyUnwantedGoods => JsString("Documentary proof that the goods are faulty or not what you ordered")
        case Correspondence => JsString("Correspondence Trader")
        case CalculationWorksheet => JsString("Calculation worksheet")
        case AdditionalSupportingDocuments => JsString("Additional supporting documents")
        case ProofOfExportOrDestruction => JsString("Proof of export or destruction")
      }

    override def reads(json: JsValue): JsResult[CE1179FileSelection] =
      json match {
        case JsString("Commercial Invoice") => JsSuccess(CommercialInvoice)
        case JsString("Import and Export Declaration") => JsSuccess(ImportAndExportDeclaration)
        case JsString("Proof of Authority (to be repaid)") => JsSuccess(LetterOfAuthority)
        case JsString("Documentary proof that the goods are faulty or not what you ordered") => JsSuccess(ProofOfFaultyUnwantedGoods)
        case JsString("Correspondence Trader") => JsSuccess(Correspondence)
        case JsString("Calculation worksheet") => JsSuccess(CalculationWorksheet)
        case JsString("Additional supporting documents") => JsSuccess(AdditionalSupportingDocuments)
        case JsString("Proof of export or destruction") => JsSuccess(ProofOfExportOrDestruction)
        case e => JsError(s"Unknown document type: $e")
      }
  }

  def options(form: Form[_])(implicit messages: Messages): Seq[RadioItem] = {
    values.map { fileType =>
      RadioItem(
        value = Some(fileType.toString),
        content = Text(messages(s"file.selection.ce1179.${fileType.toString}")),
        checked = form("value").value.contains(fileType.toString)
      )
    }
  }
}


