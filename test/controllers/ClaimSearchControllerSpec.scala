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

import connector.FinancialsApiConnector
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
    "return OK when no data found in search cache" in new Setup {
      when(mockSearchCache.get(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimSearch.onPageLoad().url)
        val result: Future[Result] = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK when data found in search cache" in new Setup {
      when(mockSearchCache.get(any))
        .thenReturn(Future.successful(Some(SearchQuery(None, "testing"))))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimSearch.onPageLoad().url)
        val result: Future[Result] = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  "onSubmit" should {
    "return BAD_REQUEST when field is empty" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest(POST, routes.ClaimSearch.onSubmit().url).withFormUrlEncodedBody("value" -> "")
      val result: Future[Result] = route(app, request).value
      status(result) mustBe BAD_REQUEST
    }

    "return a search result when the field is not empty" in new Setup {
      when(mockFinancialsApiConnector.getClaims(any, any)(any))
        .thenReturn(Future.successful(allClaims))

      when(mockSearchCache.set(any, any, any))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest(POST, routes.ClaimSearch.onSubmit().url).withFormUrlEncodedBody("value" -> "NDRC-2000")
      val result: Future[Result] = route(app, request).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.ClaimSearch.searchResult().url
    }
  }

  "searchResult" should {
    "return OK when cached search available" in new Setup {
      when(mockSearchCache.get(any))
        .thenReturn(Future.successful(Some(SearchQuery(None, "testing"))))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimSearch.searchResult().url)
        val result: Future[Result] = route(app, request).value
        status(result) mustBe OK
      }
    }

    "redirect to the search form if no cached search available" in new Setup {
      when(mockSearchCache.get(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimSearch.searchResult().url)
        val result: Future[Result] = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.ClaimSearch.onPageLoad().url
      }
    }
  }


  trait Setup {
    val mockFinancialsApiConnector: FinancialsApiConnector = mock[FinancialsApiConnector]
    val mockSearchCache: SearchCache = mock[SearchCache]

    val allClaims: AllClaims = AllClaims(
      pendingClaims = Seq(PendingClaim("NDRC-0001", C285, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))),
      inProgressClaims = Seq(InProgressClaim("NDRC-0002", C285, LocalDate.of(2019, 1, 1))),
      closedClaims = Seq(ClosedClaim("NDRC-0003", C285, LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)))
    )

    val app: Application = application.overrides(
      inject.bind[FinancialsApiConnector].toInstance(mockFinancialsApiConnector),
      inject.bind[SearchCache].toInstance(mockSearchCache)
    ).build()
  }

}
