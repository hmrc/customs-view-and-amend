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
import play.api.{Application, inject}
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, NotFoundException, ServiceUnavailableException, UpstreamErrorResponse}
import utils.SpecBase

import scala.concurrent.{ExecutionContext, Future}

class DataStoreConnectorSpec extends SpecBase {

  "Data store connector" should {
    "return existing email" in new Setup {
      val eori = "GB11111"

      val jsonResponse: String = """{"address":"someemail@mail.com"}""".stripMargin

      (mockHttp
        .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[EmailResponse],
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *, *, *)
        .returning(Future.successful(Json.parse(jsonResponse).as[EmailResponse]))

      running(app) {
        val response = connector.getEmail(eori)
        val result   = await(response)
        result shouldBe Right(Email("someemail@mail.com"))
      }
    }

    "return a UnverifiedEmail" in new Setup {
      val eori = "GB11111"
      (mockHttp
        .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[Any],
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *, *, *)
        .returning(Future.failed(UpstreamErrorResponse("NoData", 404, 404)))

      running(app) {
        val response = connector.getEmail(eori)
        await(response) shouldBe Left(UnverifiedEmail)
      }
    }

    "return a UnverifiedEmail when unexpected response occurs" in new Setup {
      val eori = "GB11111"

      val emailResponse: EmailResponse = EmailResponse(None, None, None)

      (mockHttp
        .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[EmailResponse],
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *, *, *)
        .returning(Future.successful(emailResponse))

      running(app) {
        val response = connector.getEmail(eori)
        val result   = await(response)
        result shouldBe Left(UnverifiedEmail)
      }
    }

    "return an UndeliverableEmail" in new Setup {
      val eori = "GB11111"

      val emailResponse: EmailResponse = EmailResponse(Some("email@email.com"), None, Some(JsString("")))

      (mockHttp
        .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[EmailResponse],
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *, *, *)
        .returning(Future.successful(emailResponse))

      running(app) {
        val response = connector.getEmail(eori)
        val result   = await(response)
        result shouldBe Left(UndeliverableEmail("email@email.com"))
      }
    }

    "throw service unavailable" in new Setup {
      running(app) {
        val eori = "ETMP500ERROR"
        (mockHttp
          .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
            _: HttpReads[Any],
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *, *, *)
          .returning(Future.failed(new ServiceUnavailableException("ServiceUnavailable")))

        assertThrows[ServiceUnavailableException](await(connector.getEmail(eori)))
      }
    }

    "return company name" in new Setup {
      val eori                                                   = "GB11111"
      val companyName                                            = "Company name"
      val address: CompanyAddress                                = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
      val companyInformationResponse: CompanyInformationResponse = CompanyInformationResponse(companyName, address)

      (mockHttp
        .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[CompanyInformationResponse],
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *, *, *)
        .returning(Future.successful(companyInformationResponse))

      running(app) {
        val response = connector.getCompanyName(eori)
        val result   = await(response)
        result should be(Some(companyName))
      }
    }

    "return None when no company information is found" in new Setup {
      val eori = "GB11111"
      (mockHttp
        .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
          _: HttpReads[Any],
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *, *, *)
        .returning(Future.failed(new NotFoundException("Not Found Company Information")))

      running(app) {
        val response = await(connector.getCompanyName(eori))
        response shouldBe None
      }
    }
  }

  trait Setup {
    val mockHttp: HttpClient       = mock[HttpClient]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val app = GuiceApplicationBuilder()
      .overrides(
        inject.bind[HttpClient].toInstance(mockHttp)
      )
      .configure(
        "play.filters.csp.nonce.enabled" -> "false",
        "auditing.enabled"               -> "false",
        "metrics.enabled"                -> "false"
      )
      .build()

    val connector: DataStoreConnector = app.injector.instanceOf[DataStoreConnector]
  }
}
