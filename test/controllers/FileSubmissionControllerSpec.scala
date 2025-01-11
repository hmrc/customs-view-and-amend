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

import config.AppConfig
import connector.{FileSubmissionConnector, UploadDocumentsConnector}
import models.FileSelection.AdditionalSupportingDocuments
import models.file_upload.UploadedFile
import models.{AllClaims, NDRC, PendingClaim, SessionData}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers._
import repositories.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.SpecBase

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future
import models.FileSelection

class FileSubmissionControllerSpec extends SpecBase {

  "submitFiles" should {
    "submit file to CDFpay, clear upload service state and redirect to the confirmation page" in new Setup {
      when(mockFileSubmissionConnector.submitFileToCDFPay(any, any, any, any, any, any, any)(any))
        .thenReturn(Future.successful(true))
      when(mockUploadDocumentsConnector.wipeData(any))
        .thenReturn(Future.successful(true))

      running(app) {
        val sessionData = SessionData(claims = Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withUploadedFiles(uploadedFiles)
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(GET, routes.FileSubmissionController.submitFiles.url)
        val result  = route(app, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.FileSubmissionController.showConfirmation.url

        await(sessionCache.get()) shouldBe Right(Some(sessionData.withSubmitted))
      }
    }

    "redirect straight to the confirmation page if already submitted" in new Setup {
      running(app) {
        val sessionData = SessionData(claims = Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withUploadedFiles(uploadedFiles)
          .withSubmitted
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(GET, routes.FileSubmissionController.submitFiles.url)
        val result  = route(app, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.FileSubmissionController.showConfirmation.url

        await(sessionCache.get()) shouldBe Right(Some(sessionData))
      }
    }

    "redirect back to claims overview if file upload journey not found" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.FileSubmissionController.submitFiles.url)
        val result  = route(app, request).value
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/claim-back-import-duty-vat/claims-status")
      }
    }

    "throw an exception if file upload not successful" in new Setup {
      when(mockFileSubmissionConnector.submitFileToCDFPay(any, any, any, any, any, any, any)(any))
        .thenReturn(Future.successful(false))
      when(mockUploadDocumentsConnector.wipeData(any))
        .thenReturn(Future.successful(true))

      running(app) {
        val sessionData = SessionData(claims = Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withUploadedFiles(uploadedFiles)
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(GET, routes.FileSubmissionController.submitFiles.url)
        val result  = route(app, request).value

        an[Exception] shouldBe thrownBy {
          await(result)
        }
      }
    }

    "showConfirmation" should {
      "return OK with confirmation page" in new Setup {
        running(app) {
          val session = SessionData(claims = Some(allClaimsWithPending))
            .withInitialFileUploadData("claim-123")
            .withSubmitted
          await(sessionCache.store(session)) shouldBe Right(())
          val request =
            fakeRequest(GET, routes.FileSubmissionController.showConfirmation.url)
          val result  = route(app, request).value
          status(result) mustBe OK
          await(sessionCache.get()) shouldBe Right(Some(session))
        }
      }

      "redirect back to the claims overview page if file submission not available" in new Setup {
        running(app) {
          val session = SessionData(claims = Some(allClaimsWithPending))
          await(sessionCache.store(session)) shouldBe Right(())
          val request =
            fakeRequest(GET, routes.FileSubmissionController.showConfirmation.url)
          val result  = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ClaimsOverviewController.show.url)
          await(sessionCache.get()) shouldBe Right(Some(session))
        }
      }

      "redirect back to the choose files page if files not yet submitted" in new Setup {
        running(app) {
          val session = SessionData(claims = Some(allClaimsWithPending))
            .withInitialFileUploadData("claim-123")
            .withDocumentType(FileSelection.AdditionalSupportingDocuments)
          await(sessionCache.store(session)) shouldBe Right(())
          val request =
            fakeRequest(GET, routes.FileSubmissionController.showConfirmation.url)
          val result  = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.FileUploadController.chooseFiles.url)
          await(sessionCache.get()) shouldBe Right(Some(session))
        }
      }
    }
  }

  trait Setup extends SetupBase {

    val mockFileSubmissionConnector: FileSubmissionConnector   = mock[FileSubmissionConnector]
    val mockUploadDocumentsConnector: UploadDocumentsConnector = mock[UploadDocumentsConnector]

    val app: Application = applicationWithMongoCache
      .overrides(
        bind[FileSubmissionConnector].toInstance(mockFileSubmissionConnector),
        bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector)
      )
      .build()

    def sessionCache: SessionCache = app.injector.instanceOf[SessionCache]

    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    implicit val hc: HeaderCarrier =
      HeaderCarrier(
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

    val uploadedFiles: Seq[UploadedFile] = Seq(
      UploadedFile(
        "reference",
        "/url",
        "timestamp",
        "sum",
        "file name QWERTY",
        "PDF",
        10,
        None,
        AdditionalSupportingDocuments,
        None
      )
    )

  }
}
