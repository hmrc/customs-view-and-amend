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

import models.{AllClaims, IdentifierRequest, SessionData, Error}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import scala.concurrent.{ExecutionContext, Future}

class ModifySessionActionSpec extends SpecBase {

  "ModifySessionAction" should {
    "return existing session modifier alongside the original request" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData()))))
        when(mockSessionCache.update(any)(any, any))
          .thenAnswer((f: SessionData => SessionData) => Future.successful(Right(f(SessionData()))))

        val (request, modifier) = await(modifySessionAction.transform(authenticatedRequest))
        request mustBe authenticatedRequest

        val newSession = await(modifier.update(_.copy(claims = Some(testClaims))))
        newSession mustBe Some(SessionData(Some(testClaims)))
      }
    }

    "return existing session with failing modifier alongside the original request" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData()))))
        when(mockSessionCache.update(any)(any, any))
          .thenAnswer((f: SessionData => SessionData) => Future.successful(Left(Error("do not panick"))))

        val (request, modifier) = await(modifySessionAction.transform(authenticatedRequest))
        request mustBe authenticatedRequest

        an[Exception] shouldBe thrownBy {
          await(modifier.update(_.copy(claims = Some(testClaims))))
        }
      }
    }

    "return existing session with blowing up modifier alongside the original request" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData()))))
        when(mockSessionCache.update(any)(any, any))
          .thenAnswer((f: SessionData => SessionData) => Future.failed(new Exception("do not panick")))

        val (request, modifier) = await(modifySessionAction.transform(authenticatedRequest))
        request mustBe authenticatedRequest

        an[Exception] shouldBe thrownBy {
          await(modifier.update(_.copy(claims = Some(testClaims))))
        }
      }
    }

    "return new session modifier alongside the original request" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        when(mockSessionCache.update(any)(any, any))
          .thenAnswer((f: SessionData => SessionData) => Future.successful(Right(f(SessionData()))))

        val (request, modifier) = await(modifySessionAction.transform(authenticatedRequest))
        request mustBe authenticatedRequest

        val newSession = await(modifier.update(_.copy(claims = Some(testClaims))))
        newSession mustBe Some(SessionData(Some(testClaims)))
      }
    }

    "rethrow session cache get error" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Left(Error(new Exception("do not panick")))))
        an[Exception] shouldBe thrownBy {
          await(modifySessionAction.transform(authenticatedRequest))
        }
      }
    }

    "rethrow session cache store error" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Left(Error("do not panick"))))
        an[Exception] shouldBe thrownBy {
          await(modifySessionAction.transform(authenticatedRequest))
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val app                  = application.build()
    val modifySessionAction  = app.injector.instanceOf[ModifySessionAction]
    val authenticatedRequest = IdentifierRequest(FakeRequest("GET", "/"), "someEori", Some("companyName"))

    implicit val hc: HeaderCarrier    = HeaderCarrier()
    implicit val ec: ExecutionContext = modifySessionAction.executionContext

    val testClaims: AllClaims = AllClaims(
      pendingClaims = Seq.empty,
      inProgressClaims = Seq.empty,
      closedClaims = Seq.empty
    )
  }
}
