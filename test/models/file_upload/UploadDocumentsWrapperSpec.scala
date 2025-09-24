/*
 * Copyright 2025 HM Revenue & Customs
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

package models.file_upload

import models.FileSelection.AirwayBill
import models.{FileSelection, Nonce}
import models.Nonce.Any
import play.api.libs.json.Json
import utils.SpecBase

class UploadDocumentsWrapperSpec extends SpecBase {

  private val files = Seq(
    UploadedFile(
      upscanReference = "upscanReference",
      downloadUrl = "downloadUrl",
      uploadTimestamp = "uploadTimestamp",
      checksum = "checksum",
      fileName = "fileName",
      fileMimeType = "fileMimeType",
      fileSize = 1,
      cargo = Some(UploadCargo("caseNumber")),
      description = FileSelection.AdditionalSupportingDocuments,
      previewUrl = Some("previewUrl")
    )
  )

  "UploadDocumentsWrapper" should {
    "serialize and deserialize" in {
      val config = UploadDocumentsConfig(
        nonce = Any,
        continueUrl = "continueUrl",
        callbackUrl = "callbackUrl",
        cargo = UploadCargo("caseNumber"),
        newFileDescription = AirwayBill
      )

      val uploadDocumentsWrapper = UploadDocumentsWrapper(config = config, existingFiles = files)
      val json                   = Json.toJson(uploadDocumentsWrapper)
      json shouldBe Json.obj(
        "config"        -> Json.obj(
          "nonce"              -> 0,
          "continueUrl"        -> "continueUrl",
          "callbackUrl"        -> "callbackUrl",
          "cargo"              -> Json.obj(
            "caseNumber" -> "caseNumber"
          ),
          "newFileDescription" -> "Air waybill"
        ),
        "existingFiles" -> Json.arr(
          Json.obj(
            "upscanReference" -> "upscanReference",
            "downloadUrl"     -> "downloadUrl",
            "uploadTimestamp" -> "uploadTimestamp",
            "checksum"        -> "checksum",
            "fileName"        -> "fileName",
            "fileMimeType"    -> "fileMimeType",
            "fileSize"        -> 1,
            "cargo"           -> Json.obj(
              "caseNumber" -> "caseNumber"
            ),
            "description"     -> "Additional supporting documents",
            "previewUrl"      -> "previewUrl"
          )
        )
      )

      val deserialized = json.as[UploadDocumentsWrapper]
      deserialized shouldBe uploadDocumentsWrapper
    }
  }
}
