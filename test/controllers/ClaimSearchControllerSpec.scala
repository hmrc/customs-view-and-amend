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

import connector.ClaimsConnector
import models._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.SearchCache
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimSearchControllerSpec extends SpecBase {

  "onPageLoad" should {
    "return OK" in new Setup {
      running(app) {
        val request                = fakeRequest(GET, routes.ClaimSearch.onPageLoad().url)
        val result: Future[Result] = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  "onSubmit" should {
    "return BAD_REQUEST when field is empty" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest(POST, routes.ClaimSearch.onSubmit().url).withFormUrlEncodedBody("value" -> "")
      val result: Future[Result]                           = route(app, request).value
      status(result) mustBe BAD_REQUEST
      verify(mockSearchCache, times(0)).get(any)
      verify(mockSearchCache, times(0)).set(any, any, any)
      verify(mockClaimsConnector, times(0)).getClaims(any)(any)
    }

    "return OK when the field is not empty and search cache is available" in new Setup {
      when(mockSearchCache.get(any))
        .thenReturn(Future.successful(Some(SearchQuery(allClaims, "query"))))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest(POST, routes.ClaimSearch.onSubmit().url).withFormUrlEncodedBody("search" -> "NDRC-2000")
      val result: Future[Result]                           = route(app, request).value

      status(result) mustBe OK
      verify(mockSearchCache, times(1)).get(any)
      verify(mockSearchCache, times(0)).set(any, any, any)
      verify(mockClaimsConnector, times(0)).getClaims(any)(any)
    }

    "return OK when the field is not empty and search cache is NOT available" in new Setup {
      when(mockSearchCache.get(any))
        .thenReturn(Future.successful(None))

      when(mockSearchCache.set(any, any, any))
        .thenReturn(Future.successful(true))

      when(mockClaimsConnector.getClaims(any)(any))
        .thenReturn(Future.successful(allClaims))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest(POST, routes.ClaimSearch.onSubmit().url).withFormUrlEncodedBody("search" -> "NDRC-2000")
      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      verify(mockSearchCache, times(1)).get(any)
      verify(mockSearchCache, times(1)).set(any, any, any)
      verify(mockClaimsConnector, times(1)).getClaims(any)(any)
    }
  }

  trait Setup {
    val mockClaimsConnector: ClaimsConnector = mock[ClaimsConnector]
    val mockSearchCache: SearchCache                       = mock[SearchCache]

    val allClaims: AllClaims = AllClaims(
      pendingClaims =
        Seq(PendingClaim("MRN", "NDRC-0001", NDRC, None, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))),
      inProgressClaims = Seq(InProgressClaim("MRN", "NDRC-0002", NDRC, None, LocalDate.of(2019, 1, 1))),
      closedClaims =
        Seq(ClosedClaim("MRN", "NDRC-0003", NDRC, None, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1), "Closed"))
    )

    val app: Application = application
      .overrides(
        inject.bind[ClaimsConnector].toInstance(mockClaimsConnector),
        inject.bind[SearchCache].toInstance(mockSearchCache)
      )
      .build()
  }

}
