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

package connectors

import config.AppConfig
import connector.UploadDocumentsConnector
import models.FileSelection.AdditionalSupportingDocuments
import models.file_upload.UploadDocumentsWrapper
import models.{NDRC, Nonce}
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.inject
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import utils.SpecBase

class UploadDocumentsConnectorSpec extends SpecBase with HttpV2Support {
  implicit val messages: Messages = stubMessages()

  "startFileUpload" should {
    "return the response header on a successful request" in new Setup {
      val payload = UploadDocumentsWrapper
        .createPayload(nonce, caseNumber, ndrc, documentType, previouslyUploaded)

      mockHttpPost[HttpResponse]("http://host1:123/internal/initialize", Json.toJson(payload))(
        HttpResponse(CREATED, "", Map("Location" -> Seq("/location")))
      )

      running(app) {
        val result =
          await(connector.startFileUpload(nonce, caseNumber, ndrc, documentType, previouslyUploaded))
        result shouldBe Some("/location")
      }
    }

    "return None if other status returned" in new Setup {
      val payload = UploadDocumentsWrapper
        .createPayload(nonce, caseNumber, ndrc, documentType, previouslyUploaded)

      mockHttpPost[HttpResponse]("http://host1:123/internal/initialize", Json.toJson(payload))(
        HttpResponse(NO_CONTENT, "", Map("Location" -> Seq("/location")))
      )
      running(app) {
        val result =
          await(connector.startFileUpload(nonce, caseNumber, ndrc, documentType, previouslyUploaded))
        result shouldBe None
      }
    }

    "return default UCDF location if the response header is empty" in new Setup {
      val payload = UploadDocumentsWrapper
        .createPayload(nonce, caseNumber, ndrc, documentType, previouslyUploaded)

      mockHttpPost[HttpResponse]("http://host1:123/internal/initialize", Json.toJson(payload))(
        HttpResponse(CREATED, "", Map("Location" -> Seq("/upload-customs-documents")))
      )

      running(app) {
        val result =
          await(connector.startFileUpload(nonce, caseNumber, ndrc, documentType, previouslyUploaded))
        result shouldBe Some("/upload-customs-documents")
      }
    }

    "return None if the api request fails" in new Setup {
      val payload = UploadDocumentsWrapper
        .createPayload(nonce, caseNumber, ndrc, documentType, previouslyUploaded)

      mockHttpPostWithException("http://host1:123/internal/initialize", Json.toJson(payload))(
        UpstreamErrorResponse("", 500, 500)
      )

      running(app) {
        val result =
          await(connector.startFileUpload(nonce, caseNumber, ndrc, documentType, previouslyUploaded))
        result shouldBe None
      }
    }
  }

  "wipeData" should {
    "return false on a failed response" in new Setup {
      mockHttpPostEmptyWithException("http://host1:123/internal/wipe-out")(
        UpstreamErrorResponse("", 500, 500)
      )

      running(app) {
        val result = await(connector.wipeData)
        result shouldBe false
      }
    }

    "return true on a successful response" in new Setup {
      mockHttpPostEmpty[HttpResponse]("http://host1:123/internal/wipe-out")(
        HttpResponse(NO_CONTENT, "")
      )

      running(app) {
        val result = await(connector.wipeData)
        result shouldBe true
      }
    }
  }

  trait Setup {

    val app = GuiceApplicationBuilder()
      .overrides(
        inject.bind[HttpClientV2].toInstance(mockHttp)
      )
      .configure(
        "play.filters.csp.nonce.enabled"                       -> "false",
        "auditing.enabled"                                     -> "false",
        "metrics.enabled"                                      -> "false",
        "microservice.services.upload-documents-frontend.host" -> "host1",
        "microservice.services.upload-documents-frontend.port" -> "123"
      )
      .build()

    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val connector: UploadDocumentsConnector = app.injector.instanceOf[UploadDocumentsConnector]

    val nonce              = Nonce.random
    val caseNumber         = "NDRC-1234"
    val ndrc               = NDRC
    val documentType       = AdditionalSupportingDocuments
    val previouslyUploaded = Seq.empty
  }
}
