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

package controllers

import models._
import org.mockito.Mockito
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.mvc.Result
import play.api.test.Helpers._
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimSearchControllerSpec extends SpecBase {

  "onPageLoad" should {
    "return OK" in new Setup {
      running(app) {
        val request                = fakeRequest(GET, routes.ClaimSearchController.onPageLoad.url)
        val result: Future[Result] = route(app, request).value
        status(result) mustBe OK
        verify(mockClaimsConnector, times(1)).getAllClaims(any)
      }
    }
  }

  "onSubmit" should {
    "return BAD_REQUEST when field is empty" in new Setup {
      running(app) {
        val request                = fakeRequest(POST, routes.ClaimSearchController.onSubmit.url).withFormUrlEncodedBody("value" -> "")
        val result: Future[Result] = route(app, request).value
        status(result) mustBe BAD_REQUEST
        verify(mockClaimsConnector, times(1)).getAllClaims(any)
      }
    }

    "return OK when the field is not empty" in new Setup {
      running(app) {
        val request                =
          fakeRequest(POST, routes.ClaimSearchController.onSubmit.url).withFormUrlEncodedBody("search" -> "NDRC-2000")
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        verify(mockClaimsConnector, times(1)).getAllClaims(any)
      }
    }
  }

  trait Setup extends SetupBase {

    val allClaims: AllClaims = AllClaims(
      pendingClaims =
        Seq(PendingClaim("MRN", "NDRC-0001", NDRC, None, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))),
      inProgressClaims = Seq(InProgressClaim("MRN", "NDRC-0002", NDRC, None, LocalDate.of(2019, 1, 1))),
      closedClaims =
        Seq(ClosedClaim("MRN", "NDRC-0003", NDRC, None, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), "Closed"))
    )

    val app: Application = applicationWithMongoCache.build()

    Mockito
      .lenient()
      .when(mockClaimsConnector.getAllClaims(any))
      .thenReturn(Future.successful(allClaims))
  }

}
