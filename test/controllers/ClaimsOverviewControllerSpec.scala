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
import models.{AllClaims, ClosedClaim, InProgressClaim, PendingClaim}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, inject}
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimsOverviewControllerSpec extends SpecBase {

  "show" should {
    "return OK" in new Setup {
      when(mockFinancialsApiConnector.getClaims(any))
        .thenReturn(Future.successful(allClaims))

      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.ClaimsOverview.show.url)
      val result: Future[Result] = route(app, request).value
      status(result) mustBe OK
    }
  }

  trait Setup {
    val mockFinancialsApiConnector: FinancialsApiConnector = mock[FinancialsApiConnector]

    val allClaims: AllClaims = AllClaims(
      pendingClaims = Seq(PendingClaim("NDRC-0001", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))),
      inProgressClaims = Seq(InProgressClaim("NDRC-0002", LocalDate.of(2019, 1, 1))),
      closedClaims = Seq(ClosedClaim("NDRC-0003", LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)))
    )

    val app: Application = application.overrides(
      inject.bind[FinancialsApiConnector].toInstance(mockFinancialsApiConnector)
    ).build()
  }

}