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
import models.file_upload.UploadedFile
import play.api.libs.json.{Format, JsFalse, JsNull, JsSuccess, Json, Reads, Writes}

final case class SessionData(
  verifiedEmail: Option[String] = None,
  companyName: Option[String] = None,
  xiEori: Either[Unit, Option[XiEori]] = Left(()),
  claims: Option[AllClaims] = None,
  fileUploadJourney: Option[FileUploadJourney] = None
) {

  def withVerifiedEmail(verifiedEmail: String): SessionData =
    copy(verifiedEmail = Some(verifiedEmail))

  def withCompanyName(companyName: String): SessionData =
    copy(companyName = Some(companyName))

  def withXiEori(xiEori: Option[XiEori]): SessionData =
    copy(xiEori = Right(xiEori))

  def withAllClaims(claims: AllClaims): SessionData =
    copy(claims = Some(claims))

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

  implicit val formatXiEori: Format[Either[Unit, Option[XiEori]]] =
    Format(
      Reads {
        case JsNull  => JsSuccess(Left(()))
        case JsFalse => JsSuccess(Right(None))
        case other   =>
          implicitly[Reads[XiEori]].reads(other).map(x => Right(Some(x)))
      },
      Writes {
        case Left(())            => JsNull
        case Right(None)         => JsFalse
        case Right(Some(xiEori)) =>
          implicitly[Writes[XiEori]].writes(xiEori)
      }
    )

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
