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

import models._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future
import org.mockito.Mockito

class ClaimsOverviewControllerSpec extends SpecBase {

  "show" should {
    "return OK" in new Setup {
//      Mockito
//        .lenient()
//        .when(mockClaimsConnector.getAllClaims(any)(any))
//        .thenReturn(Future.successful(allClaims))

      Mockito
        .lenient()
        .when(mockXiEoriConnector.getXiEori(any))
        .thenReturn(Future.successful(Some(XiEori("bob", "bob"))))

      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.ClaimsOverviewController.show.url)
      val result: Future[Result]                       = route(app, request).value
      status(result) mustBe OK
    }
  }

  "onSubmit" should {
    "return BAD_REQUEST when field is empty" in new Setup {
      running(app) {
        val request = fakeRequest(POST, routes.ClaimsOverviewController.onSubmit.url).withFormUrlEncodedBody("search" -> "")
        val result: Future[Result] = route(app, request).value
        status(result) mustBe BAD_REQUEST
        verify(mockClaimsConnector, times(1)).getAllClaims(any)(any)
      }
    }

    "return OK when the field is not empty" in new Setup {
      running(app) {
        val request =
          fakeRequest(POST, routes.ClaimsOverviewController.onSubmit.url).withFormUrlEncodedBody("search" -> "NDRC-0003")
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        verify(mockClaimsConnector, times(1)).getAllClaims(any)(any)
      }
    }
  }

  trait Setup extends SetupBase {

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

    val app: Application = applicationWithMongoCache.build()

    Mockito
      .lenient()
      .when(mockClaimsConnector.getAllClaims(any)(any))
      .thenReturn(Future.successful(allClaims))

    Mockito
      .lenient()
      .when(mockXiEoriConnector.getXiEori(any))
      .thenReturn(Future.successful(Some(XiEori("bob", "bob"))))
  }

}
