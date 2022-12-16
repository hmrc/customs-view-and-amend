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

import models.FileSelection.AdditionalSupportingDocuments
import models.file_upload.{Nonce, UploadedFile, UploadedFileMetadata}
import org.mongodb.scala.model.Filters
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.test.Helpers._
import utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global

class UploadFilesCacheSpec extends SpecBase {

  "initializeRecord" should {
    "populate data into the mongo with no upload documents and a stored nonce" in new Setup {
      await(for {
        _      <- database.initializeRecord(caseNumber, nonce, Seq.empty)
        result <- database.collection.find(Filters.equal("caseNumber", caseNumber)).toSingle().toFuture()
        _      <- database.collection.drop().toFuture()
      } yield {
        result.caseNumber mustBe "NDRC-2341"
        result.uploadedFilesMetadata.nonce mustBe nonce
        result.uploadedFilesMetadata.cargo mustBe None
        result.uploadedFilesMetadata.uploadedFiles mustBe Seq.empty
      })
    }
  }

  "updateRecord"                   should {
    "update the record if the case number and nonce match" in new Setup {
      await(for {
        _               <- database.initializeRecord(caseNumber, nonce, Seq.empty)
        successfulWrite <- database.updateRecord(caseNumber, validUploadedFileMetadata)
        result          <- database.retrieveCurrentlyUploadedFiles(caseNumber)
        _               <- database.collection.drop().toFuture()
      } yield {
        successfulWrite mustBe true
        result mustBe Seq(uploadedFile)
      })
    }

    "not update a record if the case number does not match" in new Setup {
      await(for {
        _               <- database.initializeRecord(caseNumber, nonce, Seq.empty)
        successfulWrite <- database.updateRecord("Invalid-case-number", validUploadedFileMetadata)
        result          <- database.retrieveCurrentlyUploadedFiles("Invalid-case-number")
        _               <- database.collection.drop().toFuture()
      } yield {
        successfulWrite mustBe false
        result mustBe Seq.empty
      })
    }

    "not update a record if the nonce does not match" in new Setup {
      await(for {
        _               <- database.initializeRecord(caseNumber, nonce, Seq.empty)
        successfulWrite <- database.updateRecord(caseNumber, validUploadedFileMetadata.copy(nonce = Nonce(123)))
        result          <- database.retrieveCurrentlyUploadedFiles(caseNumber)
        _               <- database.collection.drop().toFuture()
      } yield {
        successfulWrite mustBe false
        result mustBe Seq.empty
      })
    }
  }
  "retrieveCurrentlyUploadedFiles" should {
    "return a sequence of uploaded files if case number exists" in new Setup {
      await(for {
        _               <- database.initializeRecord(caseNumber, nonce, Seq.empty)
        successfulWrite <- database.updateRecord(caseNumber, validUploadedFileMetadata)
        result1         <- database.retrieveCurrentlyUploadedFiles(caseNumber)
        _               <- database.removeRecord(caseNumber)
        result2         <- database.retrieveCurrentlyUploadedFiles(caseNumber)
        _               <- database.collection.drop().toFuture()
      } yield {
        successfulWrite mustBe true
        result1 mustBe Seq(uploadedFile)
        result2 mustBe Seq.empty
      })
    }

    "return an empty sequence of uploaded files if case number does not exist" in new Setup {
      await(for {
        result <- database.retrieveCurrentlyUploadedFiles(caseNumber)
        _      <- database.collection.drop().toFuture()
      } yield result mustBe Seq.empty)
    }
  }

  "removeRecord" should {
    "remove a record based on the case number" in new Setup {
      await(for {
        _               <- database.initializeRecord(caseNumber, nonce, Seq.empty)
        successfulWrite <- database.updateRecord(caseNumber, validUploadedFileMetadata)
        result          <- database.retrieveCurrentlyUploadedFiles(caseNumber)
        _               <- database.collection.drop().toFuture()
      } yield {
        successfulWrite mustBe true
        result mustBe Seq(uploadedFile)
      })
    }
  }

  trait Setup extends SetupBase {
    val app: Application = application.build()

    val caseNumber: String                              = "NDRC-2341"
    val nonce: Nonce                                    = Nonce(111)
    val uploadedFile: UploadedFile                      = UploadedFile(
      "reference",
      "downloadUrl",
      "someTimestamp",
      "someChecksum",
      "someFileName",
      "mimeType",
      10,
      None,
      AdditionalSupportingDocuments,
      None
    )
    val validUploadedFileMetadata: UploadedFileMetadata = UploadedFileMetadata(nonce, Seq(uploadedFile), None)

    val database: DefaultUploadedFilesCache = app.injector.instanceOf[DefaultUploadedFilesCache]
  }

}
