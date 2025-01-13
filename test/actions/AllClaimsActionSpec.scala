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

import models.{AllClaims, ClosedClaim, Error, AuthorisedRequestWithSessionData, InProgressClaim, PendingClaim, SessionData}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase

import scala.concurrent.Future
import play.api.Application
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.http.HeaderCarrier

class AllClaimsActionSpec extends SpecBase {

  "AllClaimsAction" should {
    "return existing claims alongside the original request" in new Setup {
      running(app) {
        val response = await(allClaimsAction.transform(authorisedRequestWithClaims))
        response shouldBe ((authorisedRequestWithClaims, testClaims))
      }
    }

    "call for claims if missing in the session" in new Setup {
      running(app) {
        (mockClaimsConnector
          .getAllClaims(_: Boolean)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(testClaims))
        (mockSessionCache
          .store(_: SessionData)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Right(())))

        val response = await(allClaimsAction.transform(authorisedRequestWithoutClaims))
        response shouldBe ((authorisedRequestWithoutClaims.withAllClaims(testClaims), testClaims))
      }
    }

    "throw claims not found if claims connector error" in new Setup {
      running(app) {
        (mockClaimsConnector
          .getAllClaims(_: Boolean)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.failed(new Exception("do not panick")))
        an[AllClaimsAction.ClaimsNotFoundException] shouldBe thrownBy {
          await(allClaimsAction.transform(authorisedRequestWithoutClaims))
        }
      }
    }

    "throw claims not found if cache error when storing" in new Setup {
      running(app) {
        (mockClaimsConnector
          .getAllClaims(_: Boolean)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(testClaims))
        (mockSessionCache
          .store(_: SessionData)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.failed(new Exception("do not panick")))
        an[AllClaimsAction.ClaimsNotFoundException] shouldBe thrownBy {
          await(allClaimsAction.transform(authorisedRequestWithoutClaims))
        }
      }
    }

    "throw claims not found if cache connection error when store" in new Setup {
      running(app) {
        (mockClaimsConnector
          .getAllClaims(_: Boolean)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(testClaims))
        (mockSessionCache
          .store(_: SessionData)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Left(Error(new Exception("do not panick")))))
        an[AllClaimsAction.ClaimsNotFoundException] shouldBe thrownBy {
          await(allClaimsAction.transform(authorisedRequestWithoutClaims))
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val app: Application                 = application.build()
    val allClaimsAction: AllClaimsAction = app.injector.instanceOf[AllClaimsAction]

    val testClaims: AllClaims = AllClaims(
      pendingClaims = Seq.empty[PendingClaim],
      inProgressClaims = Seq.empty[InProgressClaim],
      closedClaims = Seq.empty[ClosedClaim]
    )

    val authorisedRequestWithClaims: AuthorisedRequestWithSessionData[AnyContentAsEmpty.type] =
      AuthorisedRequestWithSessionData(
        FakeRequest("GET", "/"),
        "someEori",
        SessionData()
          .withVerifiedEmail("foo@bar.com")
          .withCompanyName("companyName")
          .withAllClaims(testClaims)
      )

    val authorisedRequestWithoutClaims: AuthorisedRequestWithSessionData[AnyContentAsEmpty.type] =
      AuthorisedRequestWithSessionData(
        FakeRequest("GET", "/"),
        "someEori",
        SessionData()
          .withVerifiedEmail("foo@bar.com")
          .withCompanyName("companyName")
      )

  }
}
