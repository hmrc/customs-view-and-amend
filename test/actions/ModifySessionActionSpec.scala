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

import models.{AllClaims, AuthorisedRequestWithSessionData, Error, SessionData}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import scala.concurrent.{ExecutionContext, Future}

class ModifySessionActionSpec extends SpecBase {

  "ModifySessionAction" should {
    "return existing session modifier alongside the original request" in new Setup {
      running(app) {
        (mockSessionCache
          .update(_: SessionData => SessionData)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .onCall { case (f: Function1[SessionData, SessionData], _, _) =>
            Future.successful(Right(f(existingSessionData)))
          }

        val (request, modifier) = await(modifySessionAction.transform(authenticatedRequest))
        request shouldBe authenticatedRequest

        val newSession = await(modifier.update(_.copy(claims = Some(testClaims))))
        newSession shouldBe Some(existingSessionData.copy(claims = Some(testClaims)))
      }
    }

    "return existing session with failing modifier alongside the original request" in new Setup {
      running(app) {
        (mockSessionCache
          .update(_: SessionData => SessionData)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .onCall { case _ =>
            Future.successful(Left(Error("do not panick")))
          }

        val (request, modifier) = await(modifySessionAction.transform(authenticatedRequest))
        request shouldBe authenticatedRequest

        an[Exception] shouldBe thrownBy {
          await(modifier.update(_.copy(claims = Some(testClaims))))
        }
      }
    }

    "return existing session with blowing up modifier alongside the original request" in new Setup {
      running(app) {
        (mockSessionCache
          .update(_: SessionData => SessionData)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .onCall { case _ =>
            Future.failed(new Exception("do not panick"))
          }

        val (request, modifier) = await(modifySessionAction.transform(authenticatedRequest))
        request shouldBe authenticatedRequest

        an[Exception] shouldBe thrownBy {
          await(modifier.update(_.copy(claims = Some(testClaims))))
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val app                                      = application.build()
    val modifySessionAction: ModifySessionAction = app.injector.instanceOf[ModifySessionAction]

    val existingSessionData: SessionData = SessionData()
      .withVerifiedEmail("foo@bar.com")
      .withCompanyName("companyName")

    val authenticatedRequest: AuthorisedRequestWithSessionData[AnyContentAsEmpty.type] =
      AuthorisedRequestWithSessionData(
        FakeRequest("GET", "/"),
        "someEori",
        existingSessionData
      )

    implicit val hc: HeaderCarrier    = HeaderCarrier()
    implicit val ec: ExecutionContext = modifySessionAction.executionContext

    val testClaims: AllClaims = AllClaims(
      pendingClaims = Seq.empty,
      inProgressClaims = Seq.empty,
      closedClaims = Seq.empty
    )
  }
}
