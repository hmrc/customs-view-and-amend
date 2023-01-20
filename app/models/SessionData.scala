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

import cats.Eq
import models.Nonce
import models.file_upload.UploadedFile
import play.api.libs.json.{Format, Json}

final case class SessionData(claims: Option[AllClaims] = None, fileUploadJourney: Option[FileUploadJourney] = None) {

  def withInitialFileUploadData(caseNumber: String): SessionData =
    fileUploadJourney match {
      case Some(value)
          if value.claim.caseNumber == caseNumber &&
            !value.submitted =>
        this

      case _ =>
        copy(fileUploadJourney = claims.flatMap {
          _.findByCaseNumber(caseNumber).flatMap {
            case claim: PendingClaim => Some(FileUploadJourney(claim))
            case _                   => None
          }
        })
    }

  def withDocumentType(documentType: FileSelection): SessionData =
    copy(fileUploadJourney =
      fileUploadJourney
        .map(_.copy(documentType = Some(documentType)))
    )

  def withUploadedFiles(uploadedFiles: Seq[UploadedFile]): SessionData =
    copy(fileUploadJourney =
      fileUploadJourney
        .map(_.copy(previouslyUploaded = uploadedFiles))
    )

  def withSubmitted: SessionData =
    copy(fileUploadJourney =
      fileUploadJourney
        .map(_.copy(submitted = true))
    )

}

object SessionData {
  implicit val format: Format[SessionData] = Json.format[SessionData]
  implicit val eq: Eq[SessionData]         = Eq.fromUniversalEquals[SessionData]
}

final case class FileUploadJourney(
  claim: PendingClaim,
  documentType: Option[FileSelection] = None,
  previouslyUploaded: Seq[UploadedFile] = Seq.empty,
  nonce: Nonce = Nonce.random,
  submitted: Boolean = false
)

object FileUploadJourney {
  implicit val format: Format[FileUploadJourney] = Json.format[FileUploadJourney]
  implicit val eq: Eq[FileUploadJourney]         = Eq.fromUniversalEquals[FileUploadJourney]
}
