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

import models.{AuthorisedRequest, SessionData, Error}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase
import uk.gov.hmrc.auth.core.retrieve.Email
import models.email.{UndeliverableEmail, UnverifiedEmail}

import scala.concurrent.Future
import uk.gov.hmrc.http.ServiceUnavailableException
import play.api.Application
import play.api.mvc.AnyContentAsEmpty

class CurrentSessionActionSpec extends SpecBase {

  "CurrentSessionAction" should {
    "return authorised request with existing session data" in new Setup {
      val sessionData = SessionData()
        .withVerifiedEmail("foo@bar.co.uk")
        .withCompanyName("Foo Bar")
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(sessionData))))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response mustBe Right(authorisedRequest.withSessionData(sessionData))
      }
    }

    "return authorised request with new session data" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockDataStoreConnector.getEmail(any)(any))
          .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
        when(mockDataStoreConnector.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("LastMan Ltd.")))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response mustBe Right(
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
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockDataStoreConnector.getEmail(any)(any))
          .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
        when(mockDataStoreConnector.getCompanyName(any)(any))
          .thenReturn(Future.successful(None))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response mustBe Right(
          authorisedRequest.withSessionData(SessionData().withVerifiedEmail("last.man@standing.co.uk"))
        )
      }
    }

    "let request through, when getCompanyName throws service unavailable exception" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockDataStoreConnector.getEmail(any)(any))
          .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
        when(mockDataStoreConnector.getCompanyName(any)(any))
          .thenReturn(Future.failed(new ServiceUnavailableException("")))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response mustBe Right(
          authorisedRequest.withSessionData(SessionData().withVerifiedEmail("last.man@standing.co.uk"))
        )
      }
    }

    "let request through, when getEmail throws service unavailable exception" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockDataStoreConnector.getEmail(any)(any)).thenReturn(Future.failed(new ServiceUnavailableException("")))
        when(mockDataStoreConnector.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("Foo")))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(currentSessionAction.refine(authorisedRequest))
        response mustBe Right(authorisedRequest.withSessionData(SessionData().withCompanyName("Foo")))
      }
    }

    "rethrow session cache get error" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Left(Error(new Exception("do not panick")))))
        an[Exception] shouldBe thrownBy {
          await(currentSessionAction.refine(authorisedRequest))
        }
      }
    }

    "rethrow session cache store error" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockDataStoreConnector.getEmail(any)(any))
          .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
        when(mockDataStoreConnector.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("LastMan Ltd.")))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Left(Error("do not panick"))))
        an[Exception] shouldBe thrownBy {
          await(currentSessionAction.refine(authorisedRequest))
        }
      }
    }

    "display undeliverable page when getEmail returns undeliverable" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockDataStoreConnector.getEmail(any)(any))
          .thenReturn(Future.successful(Left(UndeliverableEmail("some@email.com"))))
        val response =
          await(currentSessionAction.refine(authorisedRequest)).swap.getOrElse(fail("Expected Left response"))
        response.header.status mustBe OK
      }
    }

    "redirect users with unvalidated emails" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockDataStoreConnector.getEmail(any)(any)).thenReturn(Future.successful(Left(UnverifiedEmail)))
        val response =
          await(currentSessionAction.refine(authorisedRequest)).swap.getOrElse(fail("Expected Left response"))
        response.header.status mustBe SEE_OTHER
        response.header.headers(LOCATION) must include("/verify-your-email")
      }
    }
  }

  trait Setup extends SetupBase {
    val app: Application                                             = application.build()
    val currentSessionAction: CurrentSessionAction                   = app.injector.instanceOf[CurrentSessionAction]
    val authorisedRequest: AuthorisedRequest[AnyContentAsEmpty.type] =
      AuthorisedRequest(FakeRequest("GET", "/"), "someEori")
  }
}
