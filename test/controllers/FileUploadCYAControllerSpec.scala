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

import config.AppConfig
import models.FileSelection.AdditionalSupportingDocuments
import models.file_upload.UploadedFile
import models.{IdentifierRequest, InProgressClaim, NDRC}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.{ClaimsMongo, UploadedFilesCache}
import services.ClaimService
import utils.SpecBase

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class FileUploadCYAControllerSpec extends SpecBase {

  "onPageLoad" should {
    "return OK on a successful request" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      val uploadedFiles = Seq(UploadedFile("reference", "/url", "timestamp", "sum", "file name", "PDF", 10, None, AdditionalSupportingDocuments, None))
      when(mockUploadedFilesCache.retrieveCurrentlyUploadedFiles(any))
        .thenReturn(Future.successful(uploadedFiles))

      running(app) {
        val identifierRequest: IdentifierRequest[AnyContentAsEmpty.type] =
          IdentifierRequest(fakeRequest(GET, routes.FileUploadCYAController.onPageLoad("NDRC-1234", NDRC).url), "exampleEori", Some("companyName"))
        val result = route(app, identifierRequest).value
        status(result) shouldBe OK
        contentAsString(result).contains("file name") shouldBe true
        contentAsString(result).contains("Other documents supporting your claim") shouldBe true
      }
    }

    "return NOT_FOUND when user not authorised to view claim" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.FileUploadCYAController.onPageLoad("NDRC-1234", NDRC).url)
        val result = route(app, request).value
        status(result) shouldBe NOT_FOUND
      }
    }
  }


  trait Setup {
    val claimsMongo: ClaimsMongo = ClaimsMongo(Seq(InProgressClaim("MRN", "caseNumber", NDRC, None, LocalDate.of(2021, 10, 23))), LocalDateTime.now())
    val mockClaimService: ClaimService = mock[ClaimService]
    val mockUploadedFilesCache: UploadedFilesCache = mock[UploadedFilesCache]

    val app: Application = application.overrides(
      inject.bind[ClaimService].toInstance(mockClaimService),
      inject.bind[UploadedFilesCache].toInstance(mockUploadedFilesCache)
    ).build()

    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  }
}
