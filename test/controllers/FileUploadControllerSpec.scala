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

import connector.{ClaimsConnector, FileSubmissionConnector, UploadDocumentsConnector}
import models.CaseType.Individual
import models._
import models.email.UnverifiedEmail
import models.file_upload.{Nonce, UploadCargo, UploadedFileMetadata}
import models.responses.{C285, ProcedureDetail}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.{ClaimsMongo, UploadedFilesCache}
import services.ClaimService
import uk.gov.hmrc.auth.core.retrieve.Email
import utils.SpecBase

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class FileUploadControllerSpec extends SpecBase {

  "updateFiles" should {
    "return NO_CONTENT when valid payload sent to mongo" in new Setup {
      when(mockUploadedFilesCache.updateRecord(any, any))
        .thenReturn(Future.successful(true))

      running(app) {
        val request = fakeRequest(POST, routes.FileUploadController.updateFiles().url).withJsonBody(
          Json.toJson(UploadedFileMetadata(Nonce(111), Seq.empty, Some(UploadCargo("NDRC-1000"))))
        )
        val result  = route(app, request).value
        status(result) mustBe NO_CONTENT
      }
    }

    "return BAD_REQUEST when a valid payload sent but there is no case number in the cargo" in new Setup {
      running(app) {
        val request = fakeRequest(POST, routes.FileUploadController.updateFiles().url).withJsonBody(
          Json.toJson(UploadedFileMetadata(Nonce(111), Seq.empty, None))
        )
        val result  = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "continue" should {
    "return OK" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail)))
      when(mockUploadedFilesCache.retrieveCurrentlyUploadedFiles(any))
        .thenReturn(Future.successful(Seq.empty))
      when(mockFileSubmissionConnector.submitFileToCDFPay(any, any, any, any, any, any)(any))
        .thenReturn(Future.successful(true))
      when(mockUploadedFilesCache.removeRecord(any))
        .thenReturn(Future.successful(true))
      when(mockUploadDocumentsConnector.wipeData()(any))
        .thenReturn(Future.successful(true))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.continue("NDRC-1000", NDRC).url)
        val result  = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return NOT_FOUND if the user is not authorised to view the claim" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.continue("NDRC-1000", NDRC).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND if no information returned for the specific claim" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.continue("NDRC-1000", NDRC).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND if files not successfully uploaded" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail)))
      when(mockUploadedFilesCache.retrieveCurrentlyUploadedFiles(any))
        .thenReturn(Future.successful(Seq.empty))
      when(mockFileSubmissionConnector.submitFileToCDFPay(any, any, any, any, any, any)(any))
        .thenReturn(Future.successful(false))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.continue("NDRC-1000", NDRC).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND if an email not returned a second time" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail)))
      when(mockUploadedFilesCache.retrieveCurrentlyUploadedFiles(any))
        .thenReturn(Future.successful(Seq.empty))
      when(mockFileSubmissionConnector.submitFileToCDFPay(any, any, any, any, any, any)(any))
        .thenReturn(Future.successful(true))
      when(mockUploadedFilesCache.removeRecord(any))
        .thenReturn(Future.successful(true))
      when(mockUploadDocumentsConnector.wipeData()(any))
        .thenReturn(Future.successful(true))
      when(mockDataStoreConnector.getEmail(any)(any))
        .thenReturn(Future.successful(Right(Email("email@email.com"))), Future.successful(Left(UnverifiedEmail)))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadController.continue("NDRC-1000", NDRC).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }
  }

  trait Setup {
    val mockFileSubmissionConnector: FileSubmissionConnector   = mock[FileSubmissionConnector]
    val mockClaimsConnector: ClaimsConnector     = mock[ClaimsConnector]
    val claimsMongo: ClaimsMongo                               = ClaimsMongo(
      Seq(InProgressClaim("MRN", "caseNumber", NDRC, None, LocalDate.of(2021, 10, 23))),
      LocalDateTime.now()
    )
    val mockUploadDocumentsConnector: UploadDocumentsConnector = mock[UploadDocumentsConnector]
    val mockUploadedFilesCache: UploadedFilesCache             = mock[UploadedFilesCache]
    val mockClaimService: ClaimService                         = mock[ClaimService]

    val claimDetail: ClaimDetail = ClaimDetail(
      "caseNumber",
      NDRC,
      "DeclarationId",
      Seq(ProcedureDetail("DeclarationId", true)),
      Seq.empty,
      Some("SomeLrn"),
      Some("GB746502538945"),
      InProgress,
      None,
      Some(C285),
      Some(Individual),
      LocalDate.now,
      None,
      Some("1200"),
      Some("Sarah Philips"),
      Some("sarah.philips@acmecorp.com")
    )

    when(mockDataStoreConnector.getEmail(any)(any))
      .thenReturn(Future.successful(Right(Email("some@email.com"))))

    val app: Application = application
      .overrides(
        inject.bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector),
        inject.bind[UploadedFilesCache].toInstance(mockUploadedFilesCache),
        inject.bind[ClaimsConnector].toInstance(mockClaimsConnector),
        inject.bind[FileSubmissionConnector].toInstance(mockFileSubmissionConnector),
        inject.bind[ClaimService].toInstance(mockClaimService)
      )
      .build()
  }
}
