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
import models.{C285, ClaimDetail, Closed, InProgress, Pending, PendingClaim, Security}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.ClaimsCache
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimDetailControllerSpec extends SpecBase {

  "claimDetail" should {
    "return OK when a in progress claim has been found" in new Setup {
      when(mockClaimsCache.hasCaseNumber(any, any))
        .thenReturn(Future.successful(true))
      when(mockFinancialsApiConnector.getClaimInformation(any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail)))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", Security).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK when a pending claim has been found" in new Setup {
      when(mockClaimsCache.hasCaseNumber(any, any))
        .thenReturn(Future.successful(true))
      when(mockFinancialsApiConnector.getClaimInformation(any,any)(any))
        .thenReturn(Future.successful(Some(claimDetail.copy(claimStatus = Pending))))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", C285).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK when a closed claim has been found" in new Setup {
      when(mockClaimsCache.hasCaseNumber(any, any))
        .thenReturn(Future.successful(true))
      when(mockFinancialsApiConnector.getClaimInformation(any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail.copy(claimStatus = Closed))))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", Security).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return NOT_FOUND when claim not found from the API" in new Setup {
      when(mockClaimsCache.hasCaseNumber(any, any))
        .thenReturn(Future.successful(true))
      when(mockFinancialsApiConnector.getClaimInformation(any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", Security).url)
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND when a claim is not present in the list of claims" in new Setup {
      when(mockClaimsCache.hasCaseNumber(any, any))
        .thenReturn(Future.successful(false))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", Security).url)
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }
  }

  trait Setup {
    val mockClaimsCache: ClaimsCache = mock[ClaimsCache]
    val mockFinancialsApiConnector: FinancialsApiConnector = mock[FinancialsApiConnector]

    val claimDetail: ClaimDetail = ClaimDetail(
      "caseNumber",
      Seq("21GB03I52858073821"),
      Some("GB746502538945"),
      InProgress,
      C285,
      LocalDate.of(2021, 10, 23),
      1200,
      "Sarah Philips",
      "sarah.philips@acmecorp.com"
    )

    val app: Application = application.overrides(
      inject.bind[ClaimsCache].toInstance(mockClaimsCache),
      inject.bind[FinancialsApiConnector].toInstance(mockFinancialsApiConnector)
    ).build()
  }

}
