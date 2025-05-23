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

package controllers

import connector.UploadDocumentsConnector
import models.file_upload.{UploadCargo, UploadedFile, UploadedFileMetadata}
import models.{Nonce, *}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.inject
import repositories.SessionCache
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, SessionId}
import utils.SpecBase

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class FileUploadControllerSpec extends SpecBase {

  "chooseFiles" should {
    "initialize file upload service and redirect to the returned url" in new Setup {
      (mockUploadDocumentsConnector
        .startFileUpload(
          _: Nonce,
          _: String,
          _: ServiceType,
          _: FileSelection,
          _: Seq[UploadedFile]
        )(_: HeaderCarrier, _: Messages))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(Some("/url")))

      val sessionData = SessionData(claims = Some(allClaimsWithPending))
        .withInitialFileUploadData("claim-123")
        .withDocumentType(FileSelection.AirwayBill)

      await(sessionCache.store(sessionData)) shouldBe Right(())

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.chooseFiles.url)
        val result  = route(app, request).value
        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "http://localhost:10110/url"

        await(sessionCache.get()) shouldBe Right(Some(sessionData))
      }
    }

    "initialize file upload service and redirect to the default url if missing" in new Setup {
      (mockUploadDocumentsConnector
        .startFileUpload(
          _: Nonce,
          _: String,
          _: ServiceType,
          _: FileSelection,
          _: Seq[UploadedFile]
        )(_: HeaderCarrier, _: Messages))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(None))

      val sessionData = SessionData(claims = Some(allClaimsWithPending))
        .withInitialFileUploadData("claim-123")
        .withDocumentType(FileSelection.AirwayBill)

      await(sessionCache.store(sessionData)) shouldBe Right(())

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.chooseFiles.url)
        val result  = route(app, request).value
        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe "http://localhost:10110/choose-files"

        await(sessionCache.get()) shouldBe Right(Some(sessionData))
      }
    }

    "redirect to the confirmation page if already submitted" in new Setup {
      val sessionData = SessionData(claims = Some(allClaimsWithPending))
        .withInitialFileUploadData("claim-123")
        .withDocumentType(FileSelection.AirwayBill)
        .withSubmitted

      await(sessionCache.store(sessionData)) shouldBe Right(())

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.chooseFiles.url)
        val result  = route(app, request).value
        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.FileSubmissionController.showConfirmation.url

        await(sessionCache.get()) shouldBe Right(Some(sessionData))
      }
    }

    "redirect to the file type selection page if nothing has been selected before" in new Setup {
      val sessionData = SessionData(claims = Some(allClaimsWithPending))
        .withInitialFileUploadData("claim-123")

      await(sessionCache.store(sessionData)) shouldBe Right(())

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.chooseFiles.url)
        val result  = route(app, request).value
        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.FileSelectionController.onPageLoad("claim-123").url

        await(sessionCache.get()) shouldBe Right(Some(sessionData))
      }
    }

    "redirect to the claims overview if file upload not initialized before" in new Setup {
      val sessionData = SessionData(claims = Some(allClaimsWithPending))

      await(sessionCache.store(sessionData)) shouldBe Right(())

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.chooseFiles.url)
        val result  = route(app, request).value
        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.ClaimsOverviewController.show.url

        await(sessionCache.get()) shouldBe Right(Some(sessionData))
      }
    }
  }

  "receiveUpscanCallback" should {
    "return NO_CONTENT when valid callback" in new Setup {
      running(app) {
        val sessionData = SessionData(claims = Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(
                sessionData.fileUploadJourney.get.nonce,
                uploadedFiles,
                Some(UploadCargo("NDRC-1000"))
              )
            )
          )
        val result  = route(app, request).value
        status(result) shouldBe NO_CONTENT

        await(sessionCache.get()) shouldBe
          Right(Some(sessionData.withUploadedFiles(uploadedFiles)))
      }
    }

    "return NO_CONTENT when already submitted" in new Setup {
      running(app) {
        val sessionData = SessionData(claims = Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withSubmitted
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(sessionData.fileUploadJourney.get.nonce, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) shouldBe NO_CONTENT

        await(sessionCache.get()) shouldBe Right(Some(sessionData))
      }
    }

    "return UNAUTHORIZED when session not found" in new Setup {
      running(app) {
        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(Nonce.random, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) shouldBe UNAUTHORIZED
      }
    }

    "return UNAUTHORIZED when file upload journey not found in session" in new Setup {
      running(app) {
        val sessionData = SessionData(claims = Some(allClaimsWithPending))
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(Nonce.random, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) shouldBe UNAUTHORIZED
      }
    }

    "return UNAUTHORIZED when different nonce then in session" in new Setup {
      running(app) {
        val sessionData = SessionData(claims = Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(Nonce.random, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) shouldBe UNAUTHORIZED
      }
    }

  }

  trait Setup extends SetupBase {

    stubEmailAndCompanyName

    val mockUploadDocumentsConnector: UploadDocumentsConnector =
      mock[UploadDocumentsConnector]

    val app = applicationWithMongoCache
      .overrides(
        inject.bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector)
      )
      .build()

    def sessionCache: SessionCache = app.injector.instanceOf[SessionCache]

    implicit val hc: HeaderCarrier =
      HeaderCarrier(
        authorization = Some(Authorization(UUID.randomUUID().toString)),
        sessionId = Some(SessionId(UUID.randomUUID().toString))
      )

    val allClaimsWithPending: AllClaims = AllClaims(
      pendingClaims = Seq(
        PendingClaim(
          "MRN",
          "claim-123",
          NDRC,
          None,
          Some(LocalDate.of(2021, 2, 1)),
          Some(LocalDate.of(2021, 2, 1))
        )
      ),
      inProgressClaims = Seq.empty,
      closedClaims = Seq.empty
    )

    val uploadedFiles: Seq[UploadedFile] =
      Seq(
        UploadedFile(
          upscanReference = "upscanReference",
          downloadUrl = "downloadUrl",
          uploadTimestamp = "uploadTimestamp",
          checksum = "checksum",
          fileName = "fileName",
          fileMimeType = "fileMimeType",
          fileSize = 1,
          cargo = None,
          description = FileSelection.AdditionalSupportingDocuments,
          previewUrl = Some("previewUrl")
        )
      )
  }
}
