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

import models.AuthorisedRequest
import models.email.{UndeliverableEmail, UnverifiedEmail}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.ServiceUnavailableException
import utils.SpecBase

import scala.concurrent.Future

class EmailActionSpec extends SpecBase {

  "EmailAction" should {
    "Let requests with validated email through" in new Setup {
      running(app) {
        when(mockDataStoreConnector.getEmail(any)(any))
          .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
        val response = await(emailAction.refine(authenticatedRequest))
        response mustBe Right(authenticatedRequest.withVerifiedEmail("last.man@standing.co.uk"))
      }
    }

    "Display undeliverable page when getEmail returns undeliverable" in new Setup {
      when(mockDataStoreConnector.getEmail(any)(any))
        .thenReturn(Future.successful(Left(UndeliverableEmail("some@email.com"))))
      val response = await(emailAction.refine(authenticatedRequest)).swap.getOrElse(fail("Expected Left response"))
      response.header.status mustBe OK
    }

    "Let request through, when getEmail throws service unavailable exception" in new Setup {
      running(app) {
        when(mockDataStoreConnector.getEmail(any)(any)).thenReturn(Future.failed(new ServiceUnavailableException("")))
        val response = await(emailAction.refine(authenticatedRequest))
        response mustBe Right(authenticatedRequest)
      }
    }

    "Redirect users with unvalidated emails" in new Setup {
      running(app) {
        when(mockDataStoreConnector.getEmail(any)(any)).thenReturn(Future.successful(Left(UnverifiedEmail)))
        val response = await(emailAction.refine(authenticatedRequest)).swap.getOrElse(fail("Expected Left response"))
        response.header.status mustBe SEE_OTHER
        response.header.headers(LOCATION) must include("/verify-your-email")
      }
    }
  }

  trait Setup extends SetupBase {
    val app                  = application.build()
    val emailAction          = app.injector.instanceOf[EmailAction]
    val authenticatedRequest = AuthorisedRequest(FakeRequest("GET", "/"), "someEori", Some("companyName"))
  }
}
