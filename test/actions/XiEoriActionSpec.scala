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

import models.{AllClaims, AuthorisedRequestWithSessionData, ClosedClaim, Error, InProgressClaim, PendingClaim, SessionData, XiEori}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase

import scala.concurrent.Future

class XiEoriActionSpec extends SpecBase {

  "XiEoriAction" should {
    "return xiEori" in new Setup {
      running(app) {
        val response = await(xiEoriAction.transform(authorisedRequestWithXiEori))
        response mustBe ((authorisedRequestWithXiEori, xiEori))
      }
    }

    "call for xiEori if missing in the session" in new Setup {
      running(app) {
        when(mockXiEoriConnector.getXiEori(any))
          .thenReturn(Future.successful(Some(xiEori)))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        response mustBe ((authorisedRequestWithoutXiEori.withXiEori(xiEori), xiEori))
      }
    }

    "throw xiEori not found if claims connector error" in new Setup {
      running(app) {
        when(mockXiEoriConnector.getXiEori(any))
          .thenReturn(Future.failed(new Exception("be not afraid")))
        an[XiEoriAction.XiEoriNotFoundException] shouldBe thrownBy {
          await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        }
      }
    }

    "throw xiEori not found if cache error when storing" in new Setup {
      running(app) {
        when(mockXiEoriConnector.getXiEori(any))
          .thenReturn(Future.successful(Some(xiEori)))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.failed(new Exception("be not afraid")))
        an[XiEoriAction.XiEoriNotFoundException] shouldBe thrownBy {
          await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        }
      }
    }

    "throw xiEori not found if cache connection error when store" in new Setup {
      running(app) {
        when(mockXiEoriConnector.getXiEori(any))
          .thenReturn(Future.successful(Some(xiEori)))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Left(Error(new Exception("be not afraid")))))
        an[XiEoriAction.XiEoriNotFoundException] shouldBe thrownBy {
          await(xiEoriAction.transform(authorisedRequestWithoutXiEori))
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val app             = application.build()
    val xiEoriAction    = app.injector.instanceOf[XiEoriAction]

    val xiEori: XiEori = XiEori(
      eoriXI = "XI744638982000",
      eoriGB = "GB744638982000"
    )

    val authorisedRequestWithXiEori =
      AuthorisedRequestWithSessionData(
        FakeRequest("GET", "/"),
        "someEori",
        SessionData()
          .withVerifiedEmail("foo@bar.com")
          .withCompanyName("companyName")
          .withXiEori(xiEori)
      )

    val authorisedRequestWithoutXiEori =
      AuthorisedRequestWithSessionData(
        FakeRequest("GET", "/"),
        "someEori",
        SessionData()
          .withVerifiedEmail("foo@bar.com")
          .withCompanyName("companyName")
      )

  }
}
