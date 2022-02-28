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

import models.FileSelection._
import play.api.libs.json.{JsResultException, JsString, Json}
import play.api.test.Helpers._
import utils.SpecBase

class FileSelectionSpec extends SpecBase {


  "FileSelection.toDec64FileType" should {
    "return the correct file type for uploaded files" in {
      AdditionalSupportingDocuments.toDec64FileType shouldBe "Additional Supporting Documentation"
      AirwayBill.toDec64FileType shouldBe "Air Waybill"
      AirworthinessCertificates.toDec64FileType shouldBe "Airworthiness certificates"
      BillOfLading.toDec64FileType shouldBe "Bill of Lading"
      CalculationWorksheet.toDec64FileType shouldBe "Calculation worksheet"
      CommercialInvoice.toDec64FileType shouldBe "Commercial Invoice"
      CorrespondenceTrader.toDec64FileType shouldBe "Correspondence Trader"
      ProofOfFaultyUnwantedGoods.toDec64FileType shouldBe "Documentary proof that the goods are faulty or not what you ordered"
      ImportAndExportDeclaration.toDec64FileType shouldBe "Import and Export Declaration"
      PackingList.toDec64FileType shouldBe "Packing List"
      PreferenceCertificate.toDec64FileType shouldBe "Preference certificate"
      LetterOfAuthority.toDec64FileType shouldBe "Proof of Authority (to be repaid)"
      ProofOfExportOrDestruction.toDec64FileType shouldBe "Proof of export or destruction"
      ProofOfOrigin.toDec64FileType shouldBe "Proof of origin"
      SubstituteEntry.toDec64FileType shouldBe "Substitute Entry"
      TechnicalSpecifications.toDec64FileType shouldBe "Technical specifications"
    }
  }

  "FileSelection.format" should {
    "correctly read/write document types" in {
      Json.toJson[FileSelection](AdditionalSupportingDocuments) shouldBe JsString("Additional supporting documents")
      Json.toJson[FileSelection](AirwayBill) shouldBe JsString("Air waybill")
      Json.toJson[FileSelection](AirworthinessCertificates) shouldBe JsString("Airworthiness certificates")
      Json.toJson[FileSelection](BillOfLading) shouldBe JsString("Bill of lading")
      Json.toJson[FileSelection](CalculationWorksheet) shouldBe JsString("Calculation worksheet")
      Json.toJson[FileSelection](CommercialInvoice) shouldBe JsString("Commercial invoice")
      Json.toJson[FileSelection](CorrespondenceTrader) shouldBe JsString("Correspondence between trader and agent")
      Json.toJson[FileSelection](ProofOfFaultyUnwantedGoods) shouldBe JsString("Documentary proof that the goods are faulty or not what you ordered")
      Json.toJson[FileSelection](ImportAndExportDeclaration) shouldBe JsString("Import and export declaration")
      Json.toJson[FileSelection](PackingList) shouldBe JsString("Packing list")
      Json.toJson[FileSelection](PreferenceCertificate) shouldBe JsString("Preference certificate")
      Json.toJson[FileSelection](LetterOfAuthority) shouldBe JsString("Letter of authority")
      Json.toJson[FileSelection](ProofOfExportOrDestruction) shouldBe JsString("Proof of export or destruction")
      Json.toJson[FileSelection](ProofOfOrigin) shouldBe JsString("Proof of origin")
      Json.toJson[FileSelection](SubstituteEntry) shouldBe JsString("Substitute entry")
      Json.toJson[FileSelection](TechnicalSpecifications) shouldBe JsString("Technical specifications")

      JsString("Additional supporting documents").as[FileSelection] shouldBe AdditionalSupportingDocuments
      JsString("Air waybill").as[FileSelection] shouldBe AirwayBill
      JsString("Airworthiness certificates").as[FileSelection] shouldBe AirworthinessCertificates
      JsString("Bill of lading").as[FileSelection] shouldBe BillOfLading
      JsString("Calculation worksheet").as[FileSelection] shouldBe CalculationWorksheet
      JsString("Commercial invoice").as[FileSelection] shouldBe CommercialInvoice
      JsString("Correspondence between trader and agent").as[FileSelection] shouldBe CorrespondenceTrader
      JsString("Documentary proof that the goods are faulty or not what you ordered").as[FileSelection] shouldBe ProofOfFaultyUnwantedGoods
      JsString("Import and export declaration").as[FileSelection] shouldBe ImportAndExportDeclaration
      JsString("Packing list").as[FileSelection] shouldBe PackingList
      JsString("Preference certificate").as[FileSelection] shouldBe PreferenceCertificate
      JsString("Letter of authority").as[FileSelection] shouldBe LetterOfAuthority
      JsString("Proof of export or destruction").as[FileSelection] shouldBe ProofOfExportOrDestruction
      JsString("Proof of origin").as[FileSelection] shouldBe ProofOfOrigin
      JsString("Substitute entry").as[FileSelection] shouldBe SubstituteEntry
      JsString("Technical specifications").as[FileSelection] shouldBe TechnicalSpecifications

      intercept[JsResultException] {
        JsString("INVALID").as[FileSelection]
      }.errors.map(_._2.map(_.message)).head shouldBe List("""Unknown document type: "INVALID"""")
    }
  }

  "FileSelection.messages" should {
    "return the correct messages key" in {
      AdditionalSupportingDocuments.message(stubMessages()) shouldBe "file.selection.additional-supporting-documents"
    }
  }
}
