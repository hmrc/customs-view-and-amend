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

import models.email.{UndeliverableEmail, UnverifiedEmail}
import models.{AuthorisedRequest, Error, SessionData}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.{HeaderCarrier, ServiceUnavailableException}
import utils.SpecBase

import scala.concurrent.Future

class CurrentSessionActionSpec extends SpecBase {

  "CurrentSessionAction" should {
    "return authorised request with existing session data" in new Setup {
      val sessionData = SessionData()
        .withVerifiedEmail("foo@bar.co.uk")
        .withCompanyName("Foo Bar")

      (mockSessionCache
        .get()(_: HeaderCarrier))
        .expects(*)
        .returning(Future.successful(Right(Some(sessionData))))

      running(app) {
        val response = await(currentSessionAction.refine(authorisedRequest))
        response shouldBe Right(authorisedRequest.withSessionData(sessionData))
      }
    }

    "return authorised request with new session data" in new Setup {
      (mockSessionCache
        .get()(_: HeaderCarrier))
        .expects(*)
        .returning(Future.successful(Right(None)))
      (mockDataStoreConnector
        .getEmail(_: String)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(Right(Email("last.man@standing.co.uk"))))
      (mockDataStoreConnector
        .getCompanyName(_: String)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(Some("LastMan Ltd.")))
      (mockSessionCache
        .store(_: SessionData)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(Right(())))

      running(app) {
        val response = await(currentSessionAction.refine(authorisedRequest))
        response shouldBe Right(
          authorisedRequest.withSessionData(
            SessionData()
              .withVerifiedEmail("last.man@standing.co.uk")
              .withCompanyName("LastMan Ltd.")
          )
        )
      }
    }

    "let request through, when getCompanyName returns none" in new Setup {
      running(app) {
        (mockSessionCache
          .get()(_: HeaderCarrier))
          .expects(*)
          .returning(Future.successful(Right(None)))
        (mockDataStoreConnector
          .getEmail(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Right(Email("last.man@standing.co.uk"))))
        (mockDataStoreConnector
          .getCompanyName(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(None))
        (mockSessionCache
          .store(_: SessionData)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Right(())))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response shouldBe Right(
          authorisedRequest.withSessionData(SessionData().withVerifiedEmail("last.man@standing.co.uk"))
        )
      }
    }

    "let request through, when getCompanyName throws service unavailable exception" in new Setup {
      running(app) {
        (mockSessionCache
          .get()(_: HeaderCarrier))
          .expects(*)
          .returning(Future.successful(Right(None)))
        (mockDataStoreConnector
          .getEmail(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Right(Email("last.man@standing.co.uk"))))
        (mockDataStoreConnector
          .getCompanyName(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.failed(new ServiceUnavailableException("")))
        (mockSessionCache
          .store(_: SessionData)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Right(())))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response shouldBe Right(
          authorisedRequest.withSessionData(SessionData().withVerifiedEmail("last.man@standing.co.uk"))
        )
      }
    }

    "let request through, when getEmail throws service unavailable exception" in new Setup {
      running(app) {
        (mockSessionCache
          .get()(_: HeaderCarrier))
          .expects(*)
          .returning(Future.successful(Right(None)))
        (mockDataStoreConnector
          .getEmail(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.failed(new ServiceUnavailableException("")))
        (mockDataStoreConnector
          .getCompanyName(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Some("Foo")))
        (mockSessionCache
          .store(_: SessionData)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Right(())))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response shouldBe Right(authorisedRequest.withSessionData(SessionData().withCompanyName("Foo")))
      }
    }

    "rethrow session cache get error" in new Setup {
      running(app) {
        (mockSessionCache
          .get()(_: HeaderCarrier))
          .expects(*)
          .returning(Future.successful(Left(Error(new Exception("do not panick")))))
        an[Exception] shouldBe thrownBy {
          await(currentSessionAction.refine(authorisedRequest))
        }
      }
    }

    "rethrow session cache store error" in new Setup {
      running(app) {
        (mockSessionCache
          .get()(_: HeaderCarrier))
          .expects(*)
          .returning(Future.successful(Right(None)))
        (mockDataStoreConnector
          .getEmail(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Right(Email("last.man@standing.co.uk"))))
        (mockDataStoreConnector
          .getCompanyName(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Some("LastMan Ltd.")))
        (mockSessionCache
          .store(_: SessionData)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Left(Error("do not panick"))))
        an[Exception] shouldBe thrownBy {
          await(currentSessionAction.refine(authorisedRequest))
        }
      }
    }

    "display undeliverable page when getEmail returns undeliverable" in new Setup {
      running(app) {
        (mockSessionCache
          .get()(_: HeaderCarrier))
          .expects(*)
          .returning(Future.successful(Right(None)))
        (mockDataStoreConnector
          .getEmail(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Left(UndeliverableEmail("some@email.com"))))
        val response =
          await(currentSessionAction.refine(authorisedRequest)).swap.getOrElse(fail("Expected Left response"))
        response.header.status shouldBe OK
      }
    }

    "redirect users with unvalidated emails" in new Setup {
      running(app) {
        (mockSessionCache
          .get()(_: HeaderCarrier))
          .expects(*)
          .returning(Future.successful(Right(None)))
        (mockDataStoreConnector
          .getEmail(_: String)(_: HeaderCarrier))
          .expects(*, *)
          .returning(Future.successful(Left(UnverifiedEmail)))
        val response =
          await(currentSessionAction.refine(authorisedRequest)).swap.getOrElse(fail("Expected Left response"))
        response.header.status          shouldBe SEE_OTHER
        response.header.headers(LOCATION) should include("/verify-your-email")
      }
    }
  }

  trait Setup extends SetupBase {
    val app                                                          = application.build()
    val currentSessionAction: CurrentSessionAction                   = app.injector.instanceOf[CurrentSessionAction]
    val authorisedRequest: AuthorisedRequest[AnyContentAsEmpty.type] =
      AuthorisedRequest(FakeRequest("GET", "/"), "someEori")
  }
}
