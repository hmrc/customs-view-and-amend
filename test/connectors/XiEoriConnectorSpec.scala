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
import models._
import models.responses._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, inject}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class XiEoriConnectorSpec extends SpecBase {

  "getSubscription" should {
    "return a subscription and call the backend api" in new Setup {
      when[Future[XiEoriResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(xiEoriResponse))

      running(app) {
        val result = await(connector.getXiEori)
        result    shouldBe Some(
          XiEoriResponse(
            "GB744638982000",
            "XI744638982000"
          )
        )
        result.getOrElse(fail).eoriGB shouldBe "GB744638982000"
        result.getOrElse(fail).eoriXI shouldBe "XI744638982000"
      }
    }
  }

  "getSubscription" should {
    "return an XI eori and a GB eori" in new Setup {
      when[Future[XiEoriResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(xiEoriResponse))

      running(app) {
        val result: Option[XiEoriResponse] = await(connector.getXiEori)
        result shouldBe
        result.get.eoriXI shouldBe Right("GB744638982000")
      }
    }
  }
  trait Setup extends SetupBase {
    val mockHttp: HttpClient       = mock[HttpClient]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val xiEoriResponse: XiEoriResponse = XiEoriResponse(
      "GB744638982000",
      "XI744638982000"
    )

    val startDate = Some(LocalDate.of(2021, 3, 21))

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
