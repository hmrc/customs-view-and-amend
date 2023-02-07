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

import models.{AllClaims, FileSelection, InProgressClaim, NDRC, PendingClaim, SessionData}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.i18n.Messages
import play.api.test.Helpers._
import repositories.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.SpecBase

import java.time.LocalDate
import java.util.UUID

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
        val session = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withDocumentType(FileSelection.CalculationWorksheet)
        await(sessionCache.store(session)) shouldBe Right(())
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

    "return OK with fresh journey if previous already submitted" in new Setup {
      running(app) {
        val session = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withSubmitted
        await(sessionCache.store(session)) shouldBe Right(())
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
        val session = SessionData(Some(allClaimsWithInProgress))
        await(sessionCache.store(session)) shouldBe Right(())
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
        redirectLocation(result).value mustBe routes.FileUploadController.chooseFiles.url

        await(sessionCache.get()) shouldBe
          Right(Some(sessionData.withDocumentType(FileSelection.CommercialInvoice)))
      }
    }

    "return OK on a successful request if file upload url not returned" in new Setup {
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
        redirectLocation(result).value mustBe routes.FileUploadController.chooseFiles.url

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

    "redirect to the confirmation page if already submitted" in new Setup {
      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
          .withSubmitted
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request =
          fakeRequest(POST, routes.FileSelectionController.onSubmit.url)
            .withFormUrlEncodedBody("value" -> "proof-of-authority")

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FileSubmissionController.showConfirmation.url)
      }
    }
  }

  trait Setup extends SetupBase {

    val app: Application = applicationWithMongoCache.build()

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
          Some(LocalDate.of(2021, 2, 1)),
          Some(LocalDate.of(2021, 2, 1))
        )
      )

    val inProgressClaims: Seq[InProgressClaim] =
      Seq(InProgressClaim("MRN", "claim-123", NDRC, None, Some(LocalDate.of(2021, 2, 1))))

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
