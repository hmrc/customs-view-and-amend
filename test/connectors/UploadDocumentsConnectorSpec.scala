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

package connectors

import connector.UploadDocumentsConnector
import models.responses.{AllClaimsDetail, AllClaimsResponse, SpecificClaimResponse}
import models.{C285, Security}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.UploadedFilesCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.SpecBase

import scala.concurrent.Future

class UploadDocumentsConnectorSpec extends SpecBase {

  "initializeNewFileUpload" should {
    "return the response header on a successful request" in new Setup {
      when(mockUploadDocumentsCache.initializeRecord(any, any))
        .thenReturn(Future.successful(true))

      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(CREATED, "", Map("Location" -> Seq("/location")))))

      running(app) {
        val result = await(connector.initializeNewFileUpload("NDRC-1234", C285, searched = true, multipleUpload = true))
        result shouldBe Some("/location")
      }
    }

    "return None if write to mongo fails" in new Setup {
      when(mockUploadDocumentsCache.initializeRecord(any, any))
        .thenReturn(Future.successful(false))

      running(app) {
        val result = await(connector.initializeNewFileUpload("NDRC-1234", C285, searched = true, multipleUpload = true))
        result shouldBe None
      }
    }

    "return None if other status returned" in new Setup {
      when(mockUploadDocumentsCache.initializeRecord(any, any))
        .thenReturn(Future.successful(true))

      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "", Map.empty[String, Seq[String]])))

      running(app) {
        val result = await(connector.initializeNewFileUpload("NDRC-1234", C285, searched = true, multipleUpload = true))
        result shouldBe None
      }
    }

    "return None if the response header is empty" in new Setup {
      when(mockUploadDocumentsCache.initializeRecord(any, any))
        .thenReturn(Future.successful(true))

      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(CREATED, "", Map.empty[String, Seq[String]])))

      running(app) {
        val result = await(connector.initializeNewFileUpload("NDRC-1234", C285, searched = true, multipleUpload = true))
        result shouldBe None
      }
    }

    "return None if the api request fails" in new Setup {
      when(mockUploadDocumentsCache.initializeRecord(any, any))
        .thenReturn(Future.successful(true))

      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 500, 500)))

      running(app) {
        val result = await(connector.initializeNewFileUpload("NDRC-1234", C285, searched = true, multipleUpload = true))
        result shouldBe None
      }
    }
  }

  trait Setup {
    val mockHttp: HttpClient = mock[HttpClient]
    val mockUploadDocumentsCache: UploadedFilesCache = mock[UploadedFilesCache]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val specificClaimResponse: SpecificClaimResponse = SpecificClaimResponse(
      "Pending",
      "NDRC-1234",
      Some("someEORI")
    )

    val allClaimsResponse: AllClaimsResponse = AllClaimsResponse(
      Seq(
        AllClaimsDetail("NDRC-1234", C285, "In Progress", "someEori1", "someEori2", Some("someEori3"), Some("1000"), Some("1000")),
        AllClaimsDetail("SCTY-2345", Security, "Closed", "someEori1", "someEori2", Some("someEori3"), Some("1000"), Some("1000")),
        AllClaimsDetail("NDRC-6789", C285, "Pending", "someEori1", "someEori2", Some("someEori3"), Some("1000"), Some("1000")),
      )
    )

    val app: Application = GuiceApplicationBuilder().overrides(
      inject.bind[HttpClient].toInstance(mockHttp),
      inject.bind[UploadedFilesCache].toInstance(mockUploadDocumentsCache)
    ).configure(
      "play.filters.csp.nonce.enabled" -> "false",
      "auditing.enabled" -> "false",
      "metrics.enabled" -> "false"
    ).build()

    val connector: UploadDocumentsConnector = app.injector.instanceOf[UploadDocumentsConnector]
  }
}
