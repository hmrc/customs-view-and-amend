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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Messages
import play.api.test.Helpers._
import play.api.{Application, inject}
import utils.SpecBase

import repositories.SessionCache
import models.SessionData
import uk.gov.hmrc.http.HeaderCarrier
import models.InProgressClaim
import models.NDRC
import java.time.LocalDate
import models.AllClaims
import uk.gov.hmrc.http.SessionId
import java.util.UUID
import models.PendingClaim
import scala.concurrent.Future
import models.FileSelection

class FileSelectionControllerSpec extends SpecBase {
  implicit val messages: Messages = stubMessages()

  "onPageLoad" should {
    "return OK on a successful first request" in new Setup {
      running(app) {
        await(sessionCache.store(SessionData(Some(allClaimsWithPending)))) shouldBe Right(())
        val request =
          fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim-123").url)
        val result  = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK on a successful returning request with same case number" in new Setup {
      running(app) {
        await(
          sessionCache.store(
            SessionData(Some(allClaimsWithPending))
              .withInitialFileUploadData("claim-123")
              .withDocumentType(FileSelection.CalculationWorksheet)
          )
        ) shouldBe Right(())
        val request =
          fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim-123").url)
        val result  = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK on a successful returning request with different case number" in new Setup {
      running(app) {
        await(
          sessionCache.store(
            SessionData(Some(allClaimsWithPending))
              .withInitialFileUploadData("other-claim")
          )
        ) shouldBe Right(())
        val request =
          fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim-123").url)
        val result  = route(app, request).value
        status(result) mustBe OK
      }
    }

    "redirect back to the claim details if the claim is not found" in new Setup {
      running(app) {
        val request =
          fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim-123").url)
        val result  = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/claim-back-import-duty-vat/claims-status/claim/claim-123")
      }
    }

    "redirect back to the claim details if not pending claim" in new Setup {
      running(app) {
        await(sessionCache.store(SessionData(Some(allClaimsWithInProgress)))) shouldBe Right(())
        val request =
          fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim-123").url)
        val result  = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/claim-back-import-duty-vat/claims-status/claim/claim-123")
      }
    }
  }

  "onSubmit" should {
    "return OK on a successful request" in new Setup {
      when(mockUploadDocumentsConnector.startFileUpload(any, any, any, any, any)(any, any))
        .thenReturn(Future.successful(Some("/url")))

      val sessionData = SessionData(Some(allClaimsWithPending))
        .withInitialFileUploadData("claim-123")

      await(sessionCache.store(sessionData)) shouldBe Right(())

      running(app) {
        val request = fakeRequest(POST, routes.FileSelectionController.onSubmit.url)
          .withFormUrlEncodedBody(
            "value" -> "commercial-invoice"
          )
        val result  = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe "http://localhost:10110/url"

        await(sessionCache.get()) shouldBe
          Right(Some(sessionData.withDocumentType(FileSelection.CommercialInvoice)))
      }
    }

    "return OK on a successful request if file upload url not returned" in new Setup {
      when(mockUploadDocumentsConnector.startFileUpload(any, any, any, any, any)(any, any))
        .thenReturn(Future.successful(None))

      val sessionData = SessionData(Some(allClaimsWithPending))
        .withInitialFileUploadData("claim-123")

      await(sessionCache.store(sessionData)) shouldBe Right(())

      running(app) {
        val request = fakeRequest(POST, routes.FileSelectionController.onSubmit.url)
          .withFormUrlEncodedBody(
            "value" -> "commercial-invoice"
          )
        val result  = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe "http://localhost:10110/choose-files"

        await(sessionCache.get()) shouldBe
          Right(Some(sessionData.withDocumentType(FileSelection.CommercialInvoice)))
      }
    }

    "return BAD_REQUEST if an invalid payload sent" in new Setup {
      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")

        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request =
          fakeRequest(POST, routes.FileSelectionController.onSubmit.url)
            .withFormUrlEncodedBody("value" -> "invalid-file-type")

        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to the overview if file upload journey not initialized" in new Setup {
      running(app) {
        val sessionData = SessionData(Some(allClaimsWithInProgress))
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request =
          fakeRequest(POST, routes.FileSelectionController.onSubmit.url)
            .withFormUrlEncodedBody("value" -> "proof-of-authority")

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/claim-back-import-duty-vat/claims-status")
      }
    }
  }

  trait Setup extends SetupBase {
    val mockUploadDocumentsConnector: UploadDocumentsConnector = mock[UploadDocumentsConnector]

    val app: Application = applicationWithMongoCache
      .overrides(
        inject.bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector)
      )
      .build()

    def sessionCache = app.injector.instanceOf[SessionCache]

    implicit val hc: HeaderCarrier =
      HeaderCarrier(sessionId = Some(SessionId(UUID.randomUUID().toString)))

    val pendingClaims: Seq[PendingClaim] =
      Seq(
        PendingClaim(
          "MRN",
          "claim-123",
          NDRC,
          None,
          LocalDate.of(2021, 2, 1),
          LocalDate.of(2022, 1, 1)
        )
      )

    val inProgressClaims: Seq[InProgressClaim] =
      Seq(InProgressClaim("MRN", "claim-123", NDRC, None, LocalDate.of(2021, 2, 1)))

    val allClaimsWithInProgress: AllClaims = AllClaims(
      pendingClaims = Seq.empty,
      inProgressClaims = inProgressClaims,
      closedClaims = Seq.empty
    )

    val allClaimsWithPending: AllClaims = AllClaims(
      pendingClaims = pendingClaims,
      inProgressClaims = Seq.empty,
      closedClaims = Seq.empty
    )
  }

}
