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

import connector.XiEoriConnector
import models.XiEori
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, inject}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.SpecBase

import scala.concurrent.Future

class XiEoriConnectorSpec extends SpecBase {

  "getXiEori" should {
    "return an eoriXI from backend if it exists" in new Setup {
      when[Future[HttpResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(HttpResponse(OK, validResponse)))

      running(app) {
        val result = await(connector.getXiEori)
        result shouldBe Some(
          XiEori(
            "GB744638982000",
            "XI744638982000"
          )
        )
      }
    }
    "return none if eoriXI doesn't exist" in new Setup {
      when[Future[HttpResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      running(app) {
        val result = await(connector.getXiEori)
        result shouldBe None
      }
    }
    "throw a runtime exception if there is an internal error" in new Setup {
      when[Future[HttpResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      running(app) {
        a[XiEoriConnector.Exception] shouldBe thrownBy {
          await(connector.getXiEori)
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val mockHttp: HttpClient       = mock[HttpClient]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val validResponse = """{"eoriGB":"GB744638982000","eoriXI":"XI744638982000"}"""

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

    val connector: XiEoriConnector = app.injector.instanceOf[XiEoriConnector]
  }
}
