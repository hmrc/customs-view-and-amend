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
    CommercialInvoice,
    ImportAndExportDeclaration,
    PackingList,
    AirwayBill,
    BillOfLading,
    SubstituteEntry,
    LetterOfAuthority,
    Correspondence,
    Other
  )

  implicit val enumerable: Enumerable[C285FileSelection] =
    Enumerable(values.map(v => v.toString -> v): _*)

  case object CommercialInvoice extends WithName("commercial-invoice") with C285FileSelection
  case object ImportAndExportDeclaration extends WithName("import-export-declaration") with C285FileSelection
  case object AirwayBill extends WithName("air-waybill") with C285FileSelection
  case object BillOfLading extends WithName("bill-of-lading") with C285FileSelection
  case object PackingList extends WithName("packing-list") with C285FileSelection
  case object SubstituteEntry extends WithName("substitute-entry") with C285FileSelection
  case object LetterOfAuthority extends WithName("letter-of-authority") with C285FileSelection
  case object Correspondence extends WithName("correspondence") with C285FileSelection
  case object Other extends WithName("other-documents") with C285FileSelection

  def fromString(fileType: String): C285FileSelection = {
    values.find(_.toString == fileType) match {
      case Some(value) => value
      case None => Other
    }
  }

  implicit val format: Format[C285FileSelection] = new Format[C285FileSelection] {
    override def writes(o: C285FileSelection): JsValue =
      o match {
        case CommercialInvoice => JsString("Commercial Invoice")
        case ImportAndExportDeclaration => JsString("Import and Export Declaration")
        case AirwayBill => JsString("Air Waybill")
        case BillOfLading => JsString("Bill of Lading")
        case PackingList => JsString("Packing List")
        case SubstituteEntry => JsString("Substitute Entry")
        case _ => JsString("Other documents") //TODO check whether this file type is correct for the remainder
      }

    override def reads(json: JsValue): JsResult[C285FileSelection] =
      json match {
        case JsString("Commercial Invoice") => JsSuccess(CommercialInvoice)
        case JsString("Import and Export Declaration") => JsSuccess(ImportAndExportDeclaration)
        case JsString("Air Waybill") => JsSuccess(AirwayBill)
        case JsString("Bill of Lading") => JsSuccess(BillOfLading)
        case JsString("Packing List") => JsSuccess(PackingList)
        case JsString("Substitute Entry") => JsSuccess(SubstituteEntry)
        case JsString("Other") =>  JsSuccess(Other)
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

