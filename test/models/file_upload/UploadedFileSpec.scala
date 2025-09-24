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

import models.FileSelection
import play.api.libs.json.Json
import utils.SpecBase

class UploadedFileSpec extends SpecBase {

  private val uploadedFile = UploadedFile(
    upscanReference = "upscanReference",
    downloadUrl = "downloadUrl",
    uploadTimestamp = "uploadTimestamp",
    checksum = "checksum",
    fileName = "fileName",
    fileMimeType = "fileMimeType",
    fileSize = 1,
    cargo = Some(UploadCargo(caseNumber = "caseNumber")),
    description = FileSelection.AdditionalSupportingDocuments,
    previewUrl = Some("previewUrl")
  )

  "UploadedFile" should {
    "serialize and deserialize" in {
      val json = Json.toJson(uploadedFile)
      json shouldBe Json.obj(
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

      val deserialized = json.as[UploadedFile]
      deserialized shouldBe uploadedFile
    }

    "convert to Dec64UploadedFile" in {
      val expectedFile = Dec64UploadedFile(
        upscanReference = "upscanReference",
        downloadUrl = "downloadUrl",
        uploadTimestamp = "uploadTimestamp",
        checksum = "checksum",
        fileName = "fileName",
        fileMimeType = "fileMimeType",
        fileSize = 1,
        description = FileSelection.AdditionalSupportingDocuments.toDec64FileType
      )

      uploadedFile.toDec64UploadedFile shouldBe expectedFile
    }
  }
}
