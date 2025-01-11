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

package models.file_upload

import models.FileSelection
import play.api.libs.json.{Json, OFormat}

case class UploadedFile(
  upscanReference: String,
  downloadUrl: String,
  uploadTimestamp: String,
  checksum: String,
  fileName: String,
  fileMimeType: String,
  fileSize: Int,
  cargo: Option[UploadCargo],
  description: FileSelection,
  previewUrl: Option[String]
) {

  def toDec64UploadedFile: Dec64UploadedFile =
    Dec64UploadedFile(
      upscanReference,
      downloadUrl,
      uploadTimestamp,
      checksum,
      fileName,
      fileMimeType,
      fileSize,
      description.toDec64FileType
    )

}

object UploadedFile {
  implicit val format: OFormat[UploadedFile] = Json.format[UploadedFile]
}
