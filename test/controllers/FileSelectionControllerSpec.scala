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

package controllers

import connector.UploadDocumentsConnector
import models.responses.{C285, `C&E1179`}
import models.{InProgressClaim, NDRC}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.ClaimsMongo
import services.ClaimService
import utils.SpecBase

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class FileSelectionControllerSpec extends SpecBase {

  "onPageLoad" should {
    "return NOT_FOUND if the user is not authorised to view the page" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim", NDRC, C285, initialRequest = true).url)
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return OK on a successful C285 request" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimService.clearUploaded(any, any)(any))
        .thenReturn(Future.unit)

      running(app) {
        val request = fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim", NDRC, C285, initialRequest = true).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK on a successful C&E1179 request" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimService.clearUploaded(any, any)(any))
        .thenReturn(Future.unit)

      running(app) {
        val request = fakeRequest(GET, routes.FileSelectionController.onPageLoad("claim", NDRC, `C&E1179`, initialRequest = true).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  "onSubmit" should {
    "return OK on a successful request" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockUploadDocumentsConnector.startFileUpload(any, any, any, any)(any))
        .thenReturn(Future.successful(Some("/url")))

      running(app) {
        val request = fakeRequest(POST, routes.FileSelectionController.onSubmit("claim", NDRC, `C&E1179`).url).withFormUrlEncodedBody(
          "value" -> "commercial-invoice"
        )
        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe "http://localhost:10100/url"
      }
    }

    "return BAD_REQUEST if an invalid payload sent" in new Setup {
      running(app) {
        val request = fakeRequest(POST, routes.FileSelectionController.onSubmit("claim", NDRC, C285).url).withFormUrlEncodedBody(
          "value" -> "invalid-file-type"
        )
        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST}
    }

    "return NOT_FOUND if the user not authorised to make file uploads against the claim" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(POST, routes.FileSelectionController.onSubmit("claim", NDRC, C285).url).withFormUrlEncodedBody(
          "value" -> "proof-of-origin"
        )
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND if no redirect location provided from the file upload service" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockUploadDocumentsConnector.startFileUpload(any, any, any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(POST, routes.FileSelectionController.onSubmit("claim", NDRC, C285).url).withFormUrlEncodedBody(
          "value" -> "proof-of-origin"
        )
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }
  }


  trait Setup {
    val mockClaimService: ClaimService = mock[ClaimService]
    val mockUploadDocumentsConnector: UploadDocumentsConnector = mock[UploadDocumentsConnector]

    val claimsMongo: ClaimsMongo = ClaimsMongo(Seq(InProgressClaim("MRN", "caseNumber", NDRC, None, LocalDate.of(2021, 10, 23))), LocalDateTime.now())

    val app: Application = application.overrides(
      inject.bind[ClaimService].toInstance(mockClaimService),
      inject.bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector)
    ).build()
  }

}
