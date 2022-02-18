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
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.ClaimsCache
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimListControllerSpec extends SpecBase {

  "showInProgressClaimList" should {
    "return OK" in new Setup {
      when(mockFinancialsApiConnector.getClaims(any, any)(any))
        .thenReturn(Future.successful(allClaims))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimListController.showInProgressClaimList(Some(1)).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  "showPendingClaimList" should {
    "return OK" in new Setup {
      when(mockFinancialsApiConnector.getClaims(any, any)(any))
        .thenReturn(Future.successful(allClaims))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimListController.showPendingClaimList(None).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  "showClosedClaimList" should {
    "return OK" in new Setup {
      when(mockFinancialsApiConnector.getClaims(any, any)(any))
        .thenReturn(Future.successful(allClaims))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimListController.showClosedClaimList(None).url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  trait Setup {
    val mockClaimsCache: ClaimsCache = mock[ClaimsCache]
    val mockFinancialsApiConnector: FinancialsApiConnector = mock[FinancialsApiConnector]

    val closedClaims: Seq[ClosedClaim] = (1 to 100).map { value =>
      ClosedClaim(s"NDRC-${1000 + value}", C285, LocalDate.of(2021, 2, 1).plusDays(value), LocalDate.of(2022, 1, 1).plusDays(value))
    }
    val pendingClaims: Seq[PendingClaim] = (1 to 100).map { value =>
      PendingClaim(s"NDRC-${2000 + value}", C285, LocalDate.of(2021, 2, 1).plusDays(value), LocalDate.of(2022, 1, 1).plusDays(value))
    }
    val inProgressClaim: Seq[InProgressClaim] = (1 to 100).map { value =>
      InProgressClaim(s"NDRC-${3000 + value}", C285, LocalDate.of(2021, 2, 1).plusDays(value))
    }

    val allClaims: AllClaims = AllClaims(
      pendingClaims = pendingClaims,
      inProgressClaims = inProgressClaim,
      closedClaims = closedClaims
    )

    val app: Application = application.overrides(
      inject.bind[ClaimsCache].toInstance(mockClaimsCache),
      inject.bind[FinancialsApiConnector].toInstance(mockFinancialsApiConnector)
    ).build()
  }

}
