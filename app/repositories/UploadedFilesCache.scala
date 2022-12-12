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

package repositories

import models.file_upload.{Nonce, UploadedFile, UploadedFileMetadata}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, ReplaceOptions}
import play.api.libs.json.{Format, Json, OFormat}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class DefaultUploadedFilesCache @Inject() (mongo: MongoComponent, config: Configuration)(implicit
  executionContext: ExecutionContext
) extends PlayMongoRepository[UploadedFilesMongo](
      collectionName = "uploaded-files-cache",
      mongoComponent = mongo,
      domainFormat = UploadedFilesMongo.format,
      indexes = Seq(
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions()
            .name("all-claims-cache-last-updated-index")
            .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
        ),
        IndexModel(
          ascending("uploadedFilesMetadata.nonce"),
          IndexOptions().name("nonce-index")
        ),
        IndexModel(
          ascending("caseNumber"),
          IndexOptions().name("case-index")
        )
      )
    )
    with UploadedFilesCache {

  override def initializeRecord(
    caseNumber: String,
    nonce: Nonce,
    previouslyUploaded: Seq[UploadedFile]
  ): Future[Boolean] =
    collection
      .replaceOne(
        equal("caseNumber", caseNumber),
        UploadedFilesMongo(caseNumber, UploadedFileMetadata(nonce, previouslyUploaded, None), LocalDateTime.now()),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
      .transform {
        // $COVERAGE-OFF$
        case Failure(exception) =>
          Logger(getClass).error(s"An attempt to initialize file upload records for $caseNumber resulted in $exception")
          Failure(exception)
        // $COVERAGE-ON$
        case result             => result
      }

  override def updateRecord(caseNumber: String, uploadedFileMetadata: UploadedFileMetadata): Future[Boolean] = {
    val query = Filters.and(
      equal("caseNumber", caseNumber),
      equal("uploadedFilesMetadata.nonce", uploadedFileMetadata.nonce.value)
    )
    collection
      .replaceOne(
        query,
        UploadedFilesMongo(caseNumber, uploadedFileMetadata, LocalDateTime.now())
      )
      .toFuture()
      .map(_.getModifiedCount == 1)
      .transform {
        case Failure(exception) =>
          // $COVERAGE-OFF$
          Logger(getClass).error(s"An attempt to update file upload records for $caseNumber resulted in $exception")
          Failure(exception)
        // $COVERAGE-ON$
        case result             => result
      }
  }

  override def retrieveCurrentlyUploadedFiles(caseNumber: String): Future[Seq[UploadedFile]] =
    collection
      .find(
        equal("caseNumber", caseNumber)
      )
      .toSingle()
      .toFutureOption()
      .map {
        case Some(value) => value.uploadedFilesMetadata.uploadedFiles
        case None        => Seq.empty
      }
      .transform {
        case Failure(exception) =>
          // $COVERAGE-OFF$
          Logger(getClass).error(s"An attempt to retrieve uploaded files for $caseNumber resulted in $exception")
          Failure(exception)
        // $COVERAGE-ON$
        case result             => result
      }

  override def removeRecord(caseNumber: String): Future[Boolean] =
    collection
      .deleteOne(
        equal("caseNumber", caseNumber)
      )
      .toSingle()
      .toFuture()
      .map(_.wasAcknowledged())
      .transform {
        case Failure(exception) =>
          // $COVERAGE-OFF$
          Logger(getClass).error(s"An attempt to remove file upload records for $caseNumber resulted in $exception")
          Failure(exception)
        // $COVERAGE-ON$
        case result             => result
      }
}

trait UploadedFilesCache {
  def initializeRecord(caseNumber: String, nonce: Nonce, previouslyUploaded: Seq[UploadedFile]): Future[Boolean]
  def updateRecord(caseNumber: String, uploadedFileMetadata: UploadedFileMetadata): Future[Boolean]
  def retrieveCurrentlyUploadedFiles(caseNumber: String): Future[Seq[UploadedFile]]
  def removeRecord(caseNumber: String): Future[Boolean]
}

case class UploadedFilesMongo(
  caseNumber: String,
  uploadedFilesMetadata: UploadedFileMetadata,
  lastUpdated: LocalDateTime
)

object UploadedFilesMongo {
  implicit val timeFormat: Format[LocalDateTime]   = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[UploadedFilesMongo] = Json.format[UploadedFilesMongo]
}
