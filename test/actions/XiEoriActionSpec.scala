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

package actions

import connector.XiEoriConnector
import models.{AuthorisedRequestWithSessionData, Error, SessionData, XiEori}
import play.api.Application
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import scala.concurrent.Future

class XiEoriActionSpec extends SpecBase {

  "XiEoriAction" should {
    "pass unmodified request if XI EORI is known" in new Setup {
      running(app) {
        val response = await(xiEoriAction.transform(authorisedRequestWithXiEori))
        response shouldBe authorisedRequestWithXiEori
      }
    }

    "pass unmodified request if XI EORI is known to be missing" in new Setup {
      running(app) {
        val request  = authorisedRequestWithXiEori.withXiEori(None)
        val response = await(xiEoriAction.transform(request))
        response shouldBe request
      }
    }

    "call for XI EORI when not checked yet and update the request if found" in new Setup {
      (mockXiEoriConnector
        .getXiEori(_: HeaderCarrier))
        .expects(*)
        .returning(Future.successful(Some(xiEori)))

      (mockSessionCache
        .store(_: SessionData)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(Right(())))

      running(app) {
        val response = await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        response shouldBe (authorisedRequestWithoutXiEori.withXiEori(Some(xiEori)))
      }
    }

    "call for XI EORI when not checked yet and update the request if missing" in new Setup {
      (mockXiEoriConnector
        .getXiEori(_: HeaderCarrier))
        .expects(*)
        .returning(Future.successful(None))
      (mockSessionCache
        .store(_: SessionData)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(Right(())))
      running(app) {
        val response = await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        response shouldBe authorisedRequestWithoutXiEori.withXiEori(None)
      }
    }

    "do not call for XI EORI when feature not enabled" in new Setup {
      override def includeXiClaims: Boolean = false
      running(app) {
        val response = await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        response shouldBe authorisedRequestWithoutXiEori
      }
    }

    "throw exception if XI EORI connector error" in new Setup {
      (mockXiEoriConnector
        .getXiEori(_: HeaderCarrier))
        .expects(*)
        .returning(Future.failed(new XiEoriConnector.Exception("be not afraid")))
      running(app) {
        an[XiEoriConnector.Exception] shouldBe thrownBy {
          await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        }
      }
    }

    "throw exception if session cache error when storing" in new Setup {
      (mockXiEoriConnector
        .getXiEori(_: HeaderCarrier))
        .expects(*)
        .returning(Future.successful(Some(xiEori)))
      (mockSessionCache
        .store(_: SessionData)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.failed(new Exception("be not afraid")))
      running(app) {
        an[Exception] shouldBe thrownBy {
          await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        }
      }
    }

    "throw exception if session cache connection error when store" in new Setup {
      (mockXiEoriConnector
        .getXiEori(_: HeaderCarrier))
        .expects(*)
        .returning(Future.successful(Some(xiEori)))
      (mockSessionCache
        .store(_: SessionData)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(Left(Error(new Exception("be not afraid")))))
      running(app) {
        an[Exception] shouldBe thrownBy {
          await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        }
      }
    }
  }

  trait Setup extends SetupBase {

    def includeXiClaims: Boolean = true

    val app = application
      .configure("features.include-xi-claims" -> s"$includeXiClaims")
      .build()

    val xiEoriAction: XiEoriAction = app.injector.instanceOf[XiEoriAction]

    val xiEori: XiEori = XiEori(
      eoriXI = "XI744638982000",
      eoriGB = "GB744638982000"
    )

    val authorisedRequestWithXiEori: AuthorisedRequestWithSessionData[AnyContentAsEmpty.type] =
      AuthorisedRequestWithSessionData(
        FakeRequest("GET", "/"),
        "someEori",
        SessionData()
          .withVerifiedEmail("foo@bar.com")
          .withCompanyName("companyName")
          .withXiEori(Some(xiEori))
      )

    val authorisedRequestWithoutXiEori: AuthorisedRequestWithSessionData[AnyContentAsEmpty.type] =
      AuthorisedRequestWithSessionData(
        FakeRequest("GET", "/"),
        "someEori",
        SessionData()
          .withVerifiedEmail("foo@bar.com")
          .withCompanyName("companyName")
      )

  }
}
