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

package controllers

import models.*
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimSearchControllerSpec extends SpecBase {

  "onPageLoad" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.ClaimSearchController.onPageLoad.url)
        val result  = route(app, request).value
        status(result) shouldBe OK
        // verify(mockClaimsConnector, times(1)).getAllClaims(any)(any)
      }
    }
  }

  "onSubmit" should {
    "return BAD_REQUEST when field is empty" in new Setup {
      running(app) {
        val request = fakeRequest(POST, routes.ClaimSearchController.onSubmit.url).withFormUrlEncodedBody("value" -> "")
        val result  = route(app, request).value
        status(result) shouldBe BAD_REQUEST
        // verify(mockClaimsConnector, times(1)).getAllClaims(any)(any)
      }
    }

    "redirect to claim details when the field is not empty" in new Setup {
      running(app) {
        val request =
          fakeRequest(POST, routes.ClaimSearchController.onSubmit.url).withFormUrlEncodedBody("search" -> "NDRC-0004")
        val result  = route(app, request).value

        redirectLocation(result) shouldBe Some(routes.ClaimDetailController.claimDetail("NDRC-0004").url)
        // verify(mockClaimsConnector, times(1)).getAllClaims(any)(any)
      }
    }

    "return OK when the field is not empty and multiple results found" in new Setup {
      running(app) {
        val request =
          fakeRequest(POST, routes.ClaimSearchController.onSubmit.url).withFormUrlEncodedBody("search" -> "MRN")
        val result  = route(app, request).value

        status(result) shouldBe OK
      }
    }

    "return OK when the field is not empty and query doesn't match regex for case number" in new Setup {
      running(app) {
        val request =
          fakeRequest(POST, routes.ClaimSearchController.onSubmit.url).withFormUrlEncodedBody("search" -> "foo")
        val result  = route(app, request).value

        status(result)                                                             shouldBe OK
        contentAsString(result).contains("There are no matching results for foo.") shouldBe true
      }
    }

    "redirect to claim details page when single claim found" in new Setup {
      running(app) {
        val request =
          fakeRequest(POST, routes.ClaimSearchController.onSubmit.url).withFormUrlEncodedBody("search" -> "NDRC-0001")
        val result  = route(app, request).value

        redirectLocation(result) shouldBe Some(routes.ClaimDetailController.claimDetail("NDRC-0001").url)
      }
    }
  }

  trait Setup extends SetupBase {

    stubEmailAndCompanyName

    val allClaims: AllClaims = AllClaims(
      pendingClaims = Seq(
        PendingClaim("MRN", "NDRC-0001", NDRC, None, Some(LocalDate.of(2019, 1, 1)), Some(LocalDate.of(2019, 2, 1)))
      ),
      inProgressClaims = Seq(InProgressClaim("MRN", "NDRC-0002", NDRC, None, Some(LocalDate.of(2019, 1, 1)))),
      closedClaims = Seq(
        ClosedClaim(
          "MRN",
          "NDRC-0003",
          NDRC,
          None,
          Some(LocalDate.of(2019, 1, 1)),
          Some(LocalDate.of(2019, 2, 1)),
          "Closed"
        )
      )
    )

    val app = applicationWithMongoCache.build()

    (mockClaimsConnector
      .getAllClaims(_: Boolean)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(allClaims))
  }

}
