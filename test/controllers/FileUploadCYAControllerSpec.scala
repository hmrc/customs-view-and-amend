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

import play.api.inject.bind
import config.AppConfig
import models.FileSelection.AdditionalSupportingDocuments
import models.file_upload.UploadedFile
import models.{AllClaims, NDRC, PendingClaim, SessionData}
import play.api.Application
import play.api.test.Helpers._
import repositories.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.SpecBase

import java.time.LocalDate
import java.util.UUID
import connector.FileSubmissionConnector
import connector.UploadDocumentsConnector
import scala.concurrent.Future

class FileUploadCYAControllerSpec extends SpecBase {

  "onPageLoad" should {
    "return OK on a successful request" in new Setup {
      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withUploadedFiles(uploadedFiles)
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val identifierRequest = fakeRequest(GET, routes.FileUploadCYAController.onPageLoad.url)
        val result            = route(app, identifierRequest).value

        status(result)                                                            shouldBe OK
        contentAsString(result).contains("file name QWERTY")                      shouldBe true
        contentAsString(result).contains("Other documents supporting your claim") shouldBe true
      }
    }

    "redirect back to claims overview if file upload journey to found" in new Setup {
      running(app) {
        val request =
          fakeRequest(GET, routes.FileUploadCYAController.onPageLoad.url)
        val result  = route(app, request).value
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/claim-back-import-duty-vat/claims-status")
      }
    }
  }

  "onSubmit" should {
    "return OK" in new Setup {
      when(mockFileSubmissionConnector.submitFileToCDFPay(any, any, any, any, any, any)(any))
        .thenReturn(Future.successful(true))
      when(mockUploadDocumentsConnector.wipeData(any))
        .thenReturn(Future.successful(true))

      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withUploadedFiles(uploadedFiles)
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(GET, routes.FileUploadCYAController.onSubmit.url)
        val result  = route(app, request).value

        status(result) shouldBe OK
      }
    }

    "redirect back to claims overview if file upload journey to found" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.FileUploadCYAController.onSubmit.url)
        val result  = route(app, request).value
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/claim-back-import-duty-vat/claims-status")
      }
    }

    "throw an exception if file upload not successful" in new Setup {
      when(mockFileSubmissionConnector.submitFileToCDFPay(any, any, any, any, any, any)(any))
        .thenReturn(Future.successful(false))
      when(mockUploadDocumentsConnector.wipeData(any))
        .thenReturn(Future.successful(true))

      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withUploadedFiles(uploadedFiles)
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(GET, routes.FileUploadCYAController.onSubmit.url)
        val result  = route(app, request).value

        an[Exception] shouldBe thrownBy {
          await(result)
        }
      }
    }
  }

  trait Setup extends SetupBase {

    val mockFileSubmissionConnector  = mock[FileSubmissionConnector]
    val mockUploadDocumentsConnector = mock[UploadDocumentsConnector]

    val app: Application = applicationWithMongoCache
      .overrides(
        bind[FileSubmissionConnector].toInstance(mockFileSubmissionConnector),
        bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector)
      )
      .build()

    def sessionCache = app.injector.instanceOf[SessionCache]

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
          LocalDate.of(2021, 2, 1),
          LocalDate.of(2022, 1, 1)
        )
      ),
      inProgressClaims = Seq.empty,
      closedClaims = Seq.empty
    )

    val uploadedFiles = Seq(
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
