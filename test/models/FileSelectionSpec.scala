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

import models.FileSelection._
import play.api.libs.json.{JsResultException, JsString, Json}
import play.api.test.Helpers._
import utils.SpecBase

class FileSelectionSpec extends SpecBase {

  "FileSelection.toDec64FileType" should {
    "return the correct file type for uploaded files" in {
      AdditionalSupportingDocuments.toDec64FileType shouldBe "Other"
      AirwayBill.toDec64FileType                    shouldBe "Air Waybill"
      BillOfLading.toDec64FileType                  shouldBe "Bill of Lading"
      CalculationWorksheet.toDec64FileType          shouldBe "Other"
      CommercialInvoice.toDec64FileType             shouldBe "Commercial Invoice"
      ImportAndExportDeclaration.toDec64FileType    shouldBe "Import and Export Declaration"
      PackingList.toDec64FileType                   shouldBe "Packing List"
      ProofOfAuthority.toDec64FileType              shouldBe "Proof of Authority (to be repaid)"
      SubstituteEntry.toDec64FileType               shouldBe "Substitute Entry"
    }
  }

  "FileSelection.format" should {
    "correctly read/write document types" in {
      Json.toJson[FileSelection](AdditionalSupportingDocuments) shouldBe JsString("Additional supporting documents")
      Json.toJson[FileSelection](AirwayBill)                    shouldBe JsString("Air waybill")
      Json.toJson[FileSelection](BillOfLading)                  shouldBe JsString("Bill of lading")
      Json.toJson[FileSelection](CalculationWorksheet)          shouldBe JsString("Calculation worksheet")
      Json.toJson[FileSelection](CommercialInvoice)             shouldBe JsString("Commercial invoice")
      Json.toJson[FileSelection](ImportAndExportDeclaration)    shouldBe JsString("Import and export declaration")
      Json.toJson[FileSelection](PackingList)                   shouldBe JsString("Packing list")
      Json.toJson[FileSelection](ProofOfAuthority)              shouldBe JsString("Letter of authority")
      Json.toJson[FileSelection](SubstituteEntry)               shouldBe JsString("Substitute entry")

      JsString("Additional supporting documents").as[FileSelection] shouldBe AdditionalSupportingDocuments
      JsString("Air waybill").as[FileSelection]                     shouldBe AirwayBill
      JsString("Bill of lading").as[FileSelection]                  shouldBe BillOfLading
      JsString("Calculation worksheet").as[FileSelection]           shouldBe CalculationWorksheet
      JsString("Commercial invoice").as[FileSelection]              shouldBe CommercialInvoice
      JsString("Import and export declaration").as[FileSelection]   shouldBe ImportAndExportDeclaration
      JsString("Packing list").as[FileSelection]                    shouldBe PackingList
      JsString("Letter of authority").as[FileSelection]             shouldBe ProofOfAuthority
      JsString("Substitute entry").as[FileSelection]                shouldBe SubstituteEntry

      intercept[JsResultException] {
        JsString("INVALID").as[FileSelection]
      }.errors.map(_._2.map(_.message)).head shouldBe List("""Unknown document type: "INVALID"""")
    }
  }

  "FileSelection.messages" should {
    "return the correct messages key" in {
      AdditionalSupportingDocuments.message(stubMessages()) shouldBe "file-selection.other-supporting-documents"
    }
  }
}
