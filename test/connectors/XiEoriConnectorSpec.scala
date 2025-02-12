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
import play.api.test.Helpers.*
import play.api.inject
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpResponse

import utils.SpecBase

import java.net.URL

class XiEoriConnectorSpec extends SpecBase with HttpV2Support {

  "getXiEori" should {
    "return an eoriXI from backend if it exists" in new Setup {

      mockHttpGet[HttpResponse](URL("http://host1:123/cds-reimbursement-claim/eori/xi"))(
        HttpResponse(OK, validResponse)
      )

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

      mockHttpGet[HttpResponse](URL("http://host1:123/cds-reimbursement-claim/eori/xi"))(
        HttpResponse(NO_CONTENT, "")
      )

      running(app) {
        val result = await(connector.getXiEori)
        result shouldBe None
      }
    }
    "throw a runtime exception if there is an internal error" in new Setup {
      mockHttpGet[HttpResponse](URL("http://host1:123/cds-reimbursement-claim/eori/xi"))(
        HttpResponse(INTERNAL_SERVER_ERROR, "")
      )

      running(app) {
        a[XiEoriConnector.Exception] shouldBe thrownBy {
          await(connector.getXiEori)
        }
      }
    }
  }

  trait Setup extends SetupBase {

    val validResponse = """{"eoriGB":"GB744638982000","eoriXI":"XI744638982000"}"""

    val app = GuiceApplicationBuilder()
      .overrides(
        inject.bind[HttpClientV2].toInstance(mockHttp)
      )
      .configure(
        "play.filters.csp.nonce.enabled"                     -> "false",
        "auditing.enabled"                                   -> "false",
        "metrics.enabled"                                    -> "false",
        "microservice.services.cds-reimbursement-claim.host" -> "host1",
        "microservice.services.cds-reimbursement-claim.port" -> "123"
      )
      .build()

    val connector: XiEoriConnector = app.injector.instanceOf[XiEoriConnector]
  }
}
