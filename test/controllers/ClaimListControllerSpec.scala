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
import play.api.Application
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimListControllerSpec extends SpecBase {

  "showInProgressClaimList" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.ClaimListController.showInProgressClaimList(Some(1)).url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }
  }

  "showPendingClaimList" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.ClaimListController.showPendingClaimList(None).url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }
  }

  "showClosedClaimList" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.ClaimListController.showClosedClaimList(None).url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }
  }

  trait Setup extends SetupBase {

    stubEmailAndCompanyName

    val closedClaims: Seq[ClosedClaim]        = (1 to 100).map { value =>
      ClosedClaim(
        "MRN",
        s"NDRC-${1000 + value}",
        NDRC,
        None,
        Some(LocalDate.of(2021, 2, 1).plusDays(value)),
        if (value == 7) None else Some(LocalDate.of(2022, 1, 1).plusDays(value)),
        "Closed"
      )
    }
    val pendingClaims: Seq[PendingClaim]      = (1 to 100).map { value =>
      PendingClaim(
        "MRN",
        s"NDRC-${2000 + value}",
        NDRC,
        None,
        Some(LocalDate.of(2021, 2, 1).plusDays(value)),
        Some(LocalDate.of(2022, 1, 1).plusDays(value))
      )
    }
    val inProgressClaim: Seq[InProgressClaim] = (1 to 100).map { value =>
      InProgressClaim("MRN", s"NDRC-${3000 + value}", NDRC, None, Some(LocalDate.of(2021, 2, 1).plusDays(value)))
    }

    val allClaims: AllClaims = AllClaims(
      pendingClaims = pendingClaims,
      inProgressClaims = inProgressClaim,
      closedClaims = closedClaims
    )

    val app = applicationWithMongoCache.build()

    (mockClaimsConnector
      .getAllClaims(_: Boolean)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(allClaims))

  }

}
