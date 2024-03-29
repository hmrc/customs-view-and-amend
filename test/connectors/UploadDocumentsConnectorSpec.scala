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

import connector.UploadDocumentsConnector
import models.FileSelection.AdditionalSupportingDocuments
import models.{NDRC, Nonce}
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, inject}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.SpecBase

import scala.concurrent.Future

class UploadDocumentsConnectorSpec extends SpecBase {
  implicit val messages: Messages = stubMessages()

  "startFileUpload" should {
    "return the response header on a successful request" in new Setup {
      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(CREATED, "", Map("Location" -> Seq("/location")))))

      running(app) {
        val result =
          await(connector.startFileUpload(nonce, "NDRC-1234", NDRC, AdditionalSupportingDocuments, Seq.empty))
        result shouldBe Some("/location")
      }
    }

    "return None if other status returned" in new Setup {
      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "", Map.empty[String, Seq[String]])))

      running(app) {
        val result =
          await(connector.startFileUpload(nonce, "NDRC-1234", NDRC, AdditionalSupportingDocuments, Seq.empty))
        result shouldBe None
      }
    }

    "return default UCDF location if the response header is empty" in new Setup {
      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(CREATED, "", Map.empty[String, Seq[String]])))

      running(app) {
        val result =
          await(connector.startFileUpload(nonce, "NDRC-1234", NDRC, AdditionalSupportingDocuments, Seq.empty))
        result shouldBe Some("/upload-customs-documents")
      }
    }

    "return None if the api request fails" in new Setup {
      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 500, 500)))

      running(app) {
        val result =
          await(connector.startFileUpload(nonce, "NDRC-1234", NDRC, AdditionalSupportingDocuments, Seq.empty))
        result shouldBe None
      }
    }
  }

  "wipeData" should {
    "return false on a failed response" in new Setup {
      when[Future[HttpResponse]](mockHttp.POSTEmpty(any, any)(any, any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 500, 500)))

      running(app) {
        val result = await(connector.wipeData)
        result shouldBe false
      }
    }

    "return true on a successful response" in new Setup {
      when[Future[HttpResponse]](mockHttp.POSTEmpty(any, any)(any, any, any))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      running(app) {
        val result = await(connector.wipeData)
        result shouldBe true
      }
    }
  }

  trait Setup {
    val mockHttp: HttpClient = mock[HttpClient]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val app: Application = GuiceApplicationBuilder()
      .overrides(
        inject.bind[HttpClient].toInstance(mockHttp)
      )
      .configure(
        "play.filters.csp.nonce.enabled" -> "false",
        "auditing.enabled"               -> "false",
        "metrics.enabled"                -> "false"
      )
      .build()

    val connector: UploadDocumentsConnector = app.injector.instanceOf[UploadDocumentsConnector]

    val nonce = Nonce.random
  }
}
