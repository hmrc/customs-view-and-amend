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

import connector.{FinancialsApiConnector, UploadDocumentsConnector}
import models.file_upload.{Nonce, UploadCargo, UploadedFileMetadata}
import models.{AllClaims, C285, ClaimDetail, ClosedClaim, InProgress, InProgressClaim, Pending, PendingClaim}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.{ClaimsCache, ClaimsMongo, UploadedFilesCache}
import uk.gov.hmrc.auth.core.retrieve.Email
import utils.SpecBase

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class FileUploadControllerSpec extends SpecBase {

  "start" should {
    "redirect the user to the file upload service on a successful request" in new Setup {
      when(mockFinancialsApiConnector.getClaims(any)(any))
        .thenReturn(Future.successful(AllClaims(Seq.empty, Seq.empty, Seq.empty)))
      when(mockClaimsCache.getSpecificCase(any, any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockUploadDocumentsConnector.initializeNewFileUpload(any, any, any, any)(any))
        .thenReturn(Future.successful(Some("/location")))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.start("NDRC-100", C285, searched = true, multipleUpload = true).url)
        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe "http://localhost:10100/location"
      }
    }

    "return NOT_FOUND if the specific case number does not belong to the user" in new Setup {
      when(mockClaimsCache.getSpecificCase(any, any))
        .thenReturn(Future.successful(None))
      when(mockFinancialsApiConnector.getClaims(any)(any))
        .thenReturn(Future.successful(AllClaims(Seq.empty, Seq.empty, Seq.empty)))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.start("NDRC-100", C285, searched = true, multipleUpload = true).url)
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND if the request to initialize the file upload service fails" in new Setup {
      when(mockClaimsCache.getSpecificCase(any, any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockUploadDocumentsConnector.initializeNewFileUpload(any, any, any, any)(any))
        .thenReturn(Future.successful(None))
      when(mockFinancialsApiConnector.getClaims(any)(any))
        .thenReturn(Future.successful(AllClaims(Seq.empty, Seq.empty, Seq.empty)))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.start("NDRC-100", C285, searched = true, multipleUpload = true).url)
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }
  }

  "updateFiles" should {
    "return NO_CONTENT when valid payload sent to mongo" in new Setup {
      when(mockUploadedFilesCache.updateRecord(any, any))
        .thenReturn(Future.successful(true))

      running(app) {
        val request = fakeRequest(POST, routes.FileUploadController.updateFiles().url).withJsonBody(
          Json.toJson(UploadedFileMetadata(Nonce.toNonce(111), Seq.empty, Some(UploadCargo("NDRC-1000"))))
        )
        val result = route(app, request).value
        status(result) mustBe NO_CONTENT
      }
    }

    "return BAD_REQUEST when a valid payload sent but there is no case number in the cargo" in new Setup {
      running(app) {
        val request = fakeRequest(POST, routes.FileUploadController.updateFiles().url).withJsonBody(
          Json.toJson(UploadedFileMetadata(Nonce.toNonce(111), Seq.empty, None))
        )
        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "continue" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.continue("NDRC-1000").url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }
  }


  trait Setup {
    val mockClaimsCache: ClaimsCache = mock[ClaimsCache]
    val mockFinancialsApiConnector: FinancialsApiConnector = mock[FinancialsApiConnector]
    val claimsMongo: ClaimsMongo = ClaimsMongo(Seq(InProgressClaim("caseNumber", C285, LocalDate.of(2021, 10, 23))), LocalDateTime.now())
    val mockUploadDocumentsConnector: UploadDocumentsConnector = mock[UploadDocumentsConnector]
    val mockUploadedFilesCache: UploadedFilesCache = mock[UploadedFilesCache]

    val claimDetail: ClaimDetail = ClaimDetail(
      "caseNumber",
      Seq("21GB03I52858073821"),
      "SomeLrn",
      Some("GB746502538945"),
      InProgress,
      C285,
      LocalDate.of(2021, 10, 23),
      1200,
      "Sarah Philips",
      "sarah.philips@acmecorp.com"
    )

    when(mockDataStoreConnector.getEmail(any)(any))
      .thenReturn(Future.successful(Right(Email("some@email.com"))))

    val app: Application = application.overrides(
      inject.bind[ClaimsCache].toInstance(mockClaimsCache),
      inject.bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector),
      inject.bind[UploadedFilesCache].toInstance(mockUploadedFilesCache),
      inject.bind[FinancialsApiConnector].toInstance(mockFinancialsApiConnector)
    ).build()
  }
}
