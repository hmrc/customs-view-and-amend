/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{AllClaims, ClosedClaim, Error, IdentifierRequest, InProgressClaim, PendingClaim, SessionData}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.SessionRecordNotFound
import utils.SpecBase

import scala.concurrent.Future

class AllClaimsActionSpec extends SpecBase {

  "AllClaimsAction" should {
    "return existing claims alongside the original request" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData(Some(testClaims))))))
        val response = await(allClaimsAction.transform(authenticatedRequest))
        response mustBe ((authenticatedRequest, testClaims))
      }
    }

    "call for claims if missing in the session" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData(None)))))
        when(mockClaimsConnector.getAllClaims(any))
          .thenReturn(Future.successful(testClaims))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(allClaimsAction.transform(authenticatedRequest))
        response mustBe ((authenticatedRequest, testClaims))
      }
    }

    "create a new session and call for claims if missing" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockClaimsConnector.getAllClaims(any))
          .thenReturn(Future.successful(testClaims))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(allClaimsAction.transform(authenticatedRequest))
        response mustBe ((authenticatedRequest, testClaims))
      }
    }

    "throw session record not found if session error" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Left(Error(new Exception("do not panick")))))
        an[Exception] shouldBe thrownBy {
          await(allClaimsAction.transform(authenticatedRequest))
        }
      }
    }

    "throw session record not found if cache connection error when get" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any)).thenReturn(Future.failed(new Exception("do not panick")))
        an[SessionRecordNotFound] shouldBe thrownBy {
          await(allClaimsAction.transform(authenticatedRequest))
        }
      }
    }

    "throw claims not found if claims connector error" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData(None)))))
        when(mockClaimsConnector.getAllClaims(any))
          .thenReturn(Future.failed(new Exception("do not panick")))
        an[AllClaimsAction.ClaimsNotFoundException] shouldBe thrownBy {
          await(allClaimsAction.transform(authenticatedRequest))
        }
      }
    }

    "throw claims not found if cache error when storing" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData(None)))))
        when(mockClaimsConnector.getAllClaims(any))
          .thenReturn(Future.successful(testClaims))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.failed(new Exception("do not panick")))
        an[AllClaimsAction.ClaimsNotFoundException] shouldBe thrownBy {
          await(allClaimsAction.transform(authenticatedRequest))
        }
      }
    }

    "throw claims not found if cache connection error when store" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData(None)))))
        when(mockClaimsConnector.getAllClaims(any))
          .thenReturn(Future.successful(testClaims))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Left(Error(new Exception("do not panick")))))
        an[AllClaimsAction.ClaimsNotFoundException] shouldBe thrownBy {
          await(allClaimsAction.transform(authenticatedRequest))
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val app                  = application.build()
    val allClaimsAction      = app.injector.instanceOf[AllClaimsAction]
    val authenticatedRequest = IdentifierRequest(FakeRequest("GET", "/"), "someEori", Some("companyName"))

    val testClaims: AllClaims = AllClaims(
      pendingClaims = Seq.empty[PendingClaim],
      inProgressClaims = Seq.empty[InProgressClaim],
      closedClaims = Seq.empty[ClosedClaim]
    )
  }
}
