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

import connector.DataStoreConnector
import models.company.{CompanyAddress, CompanyInformationResponse}
import models.email.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers.*
import play.api.inject
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{NotFoundException, ServiceUnavailableException, UpstreamErrorResponse}
import utils.SpecBase
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

import java.net.URL

class DataStoreConnectorSpec extends SpecBase with HttpV2Support {

  "Data store connector" should {
    "return existing email" in new Setup {
      val eori                 = "GB11111"
      val body: JsValue        = Json.obj("eori" -> eori)
      val jsonResponse: String = """{"address":"someemail@mail.com"}""".stripMargin

      val expectedResponse: EmailResponse = Json.parse(jsonResponse).as[EmailResponse]

      mockHttpPost[EmailResponse](s"http://host1:123/customs-data-store/eori/verified-email-third-party", body)(
        expectedResponse
      )

      running(app) {
        val response = connector.getEmail(eori)
        val result   = await(response)
        result shouldBe Right(Email("someemail@mail.com"))
      }
    }

    "return a UnverifiedEmail" in new Setup {
      val eori          = "GB11111"
      val body: JsValue = Json.obj("eori" -> eori)
      mockHttpPostWithException(s"http://host1:123/customs-data-store/eori/verified-email-third-party", body)(
        new UpstreamErrorResponse("NoData", 404, 404, Map.empty)
      )

      running(app) {
        val response = connector.getEmail(eori)
        await(response) shouldBe Left(UnverifiedEmail)
      }
    }

    "return a UnverifiedEmail when unexpected response occurs" in new Setup {
      val eori                         = "GB11111"
      val body: JsValue                = Json.obj("eori" -> eori)
      val emailResponse: EmailResponse = EmailResponse(None, None, None)

      mockHttpPost[EmailResponse](s"http://host1:123/customs-data-store/eori/verified-email-third-party", body)(
        emailResponse
      )

      running(app) {
        val response = connector.getEmail(eori)
        val result   = await(response)
        result shouldBe Left(UnverifiedEmail)
      }
    }

    "return an UndeliverableEmail" in new Setup {
      val eori                         = "GB11111"
      val body: JsValue                = Json.obj("eori" -> eori)
      val emailResponse: EmailResponse = EmailResponse(Some("email@email.com"), None, Some(JsString("")))

      mockHttpPost[EmailResponse](s"http://host1:123/customs-data-store/eori/verified-email-third-party", body)(
        emailResponse
      )

      running(app) {
        val response = connector.getEmail(eori)
        val result   = await(response)
        result shouldBe Left(UndeliverableEmail("email@email.com"))
      }
    }

    "throw service unavailable" in new Setup {
      running(app) {
        val eori          = "ETMP500ERROR"
        val body: JsValue = Json.obj("eori" -> eori)
        mockHttpPostWithException(s"http://host1:123/customs-data-store/eori/verified-email-third-party", body)(
          new ServiceUnavailableException("ServiceUnavailable")
        )

        assertThrows[ServiceUnavailableException](await(connector.getEmail(eori)))
      }
    }

    "return company name" in new Setup {
      val eori                                                   = "GB11111"
      val companyName                                            = "Company name"
      val address: CompanyAddress                                = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
      val companyInformationResponse: CompanyInformationResponse = CompanyInformationResponse(companyName, address)
      val body: JsValue                                          = Json.obj("eori" -> eori)
      mockHttpPost[CompanyInformationResponse](
        s"http://host1:123/customs-data-store/eori/company-information-third-party",
        body
      )(
        companyInformationResponse
      )

      running(app) {
        val response = connector.getCompanyName(eori)
        val result   = await(response)
        result should be(Some(companyName))
      }
    }

    "return None when no company information is found" in new Setup {
      val eori          = "GB11111"
      val body: JsValue = Json.obj("eori" -> eori)
      mockHttpPostWithException(s"http://host1:123/customs-data-store/eori/company-information-third-party", body)(
        new NotFoundException("Not Found Company Information")
      )

      running(app) {
        val response = await(connector.getCompanyName(eori))
        response shouldBe None
      }
    }
  }

  trait Setup {

    val app = GuiceApplicationBuilder()
      .overrides(
        inject.bind[HttpClientV2].toInstance(mockHttp)
      )
      .configure(
        "play.filters.csp.nonce.enabled"                -> "false",
        "auditing.enabled"                              -> "false",
        "metrics.enabled"                               -> "false",
        "microservice.services.customs-data-store.host" -> "host1",
        "microservice.services.customs-data-store.port" -> "123"
      )
      .build()

    val connector: DataStoreConnector = app.injector.instanceOf[DataStoreConnector]
  }
}
