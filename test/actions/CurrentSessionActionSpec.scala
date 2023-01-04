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

import models.{IdentifierRequest, SessionData, Error}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase

import scala.concurrent.Future

class CurrentSessionActionSpec extends SpecBase {

  "CurrentSessionAction" should {
    "return existing session alongside the original request" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(Some(SessionData()))))
        val response = await(currentSessionAction.transform(authenticatedRequest))
        response mustBe ((authenticatedRequest, SessionData()))
      }
    }

    "return new session if missing alongside the original request" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Right(None)))
        when(mockSessionCache.store(any)(any))
          .thenReturn(Future.successful(Right(())))
        val response = await(currentSessionAction.transform(authenticatedRequest))
        response mustBe ((authenticatedRequest, SessionData()))
      }
    }

    "rethrow session cache get error" in new Setup {
      running(app) {
        when(mockSessionCache.get()(any))
          .thenReturn(Future.successful(Left(Error(new Exception("do not panick")))))
        an[Exception] shouldBe thrownBy {
          await(currentSessionAction.transform(authenticatedRequest))
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
          await(currentSessionAction.transform(authenticatedRequest))
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val app                  = application.build()
    val currentSessionAction = app.injector.instanceOf[CurrentSessionAction]
    val authenticatedRequest = IdentifierRequest(FakeRequest("GET", "/"), "someEori", Some("companyName"))
  }
}
