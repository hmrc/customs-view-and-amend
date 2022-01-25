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

package connectors

import connector.FinancialsApiConnector
import models._
import models.responses.{AllClaimsDetail, AllClaimsResponse, SpecificClaimResponse}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.ClaimsCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class FinancialsApiConnectorSpec extends SpecBase {

  "getClaims" should {
    "return AllClaims and call the financials api if no cached data present" in new Setup {
      when[Future[AllClaimsResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(allClaimsResponse))

      when(mockClaimCache.get(any))
        .thenReturn(Future.successful(None))
      when(mockClaimCache.set(any, any))
        .thenReturn(Future.successful(true))

      running(app) {
        val result = await(connector.getClaims("someEori"))
        result.closedClaims shouldBe Seq(ClosedClaim("SCTY-2345", Security, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 2, 1)))
        result.inProgressClaims shouldBe Seq(InProgressClaim("NDRC-1234", C285, LocalDate.of(9999, 1, 1)))
        result.pendingClaims shouldBe Seq(PendingClaim("NDRC-6789", C285, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 1, 1)))
      }
    }

    "return AllClaims and not call the financials api if cached data present" in new Setup {
      when(mockClaimCache.get(any))
        .thenReturn(Future.successful(Some(Seq(
          ClosedClaim("SCTY-2345", Security, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 2, 1)),
          InProgressClaim("NDRC-1234", C285, LocalDate.of(9999, 1, 1)),
          PendingClaim("NDRC-6789", C285, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 1, 1))
        ))))

      running(app) {
        val result = await(connector.getClaims("someEori"))
        result.closedClaims shouldBe Seq(ClosedClaim("SCTY-2345", Security, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 2, 1)))
        result.inProgressClaims shouldBe Seq(InProgressClaim("NDRC-1234", C285, LocalDate.of(9999, 1, 1)))
        result.pendingClaims shouldBe Seq(PendingClaim("NDRC-6789", C285, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 1, 1)))
      }
    }
  }

  "getClaimInformation" should {
    "return ClaimDetail when a data returned from the API" in new Setup {
      when[Future[SpecificClaimResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(specificClaimResponse))

      running(app) {
        val result = await(connector.getClaimInformation("NDRC-1234", Security)).value
        result.claimantsEori.value shouldBe "someEORI"
        result.claimType shouldBe Security
        result.mrn shouldBe Seq("AWAITING API SPEC")
        result.claimStatus shouldBe Pending
        result.caseNumber shouldBe "NDRC-1234"
        result.claimStartDate shouldBe LocalDate.of(9999, 1, 1)
        result.claimantsEmail shouldBe "AWAITING API SPEC"
        result.claimantsName shouldBe "AWAITING API SPEC"
      }
    }

    "return None when NO_CONTENT returned from the API" in new Setup {
      when[Future[SpecificClaimResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 500, 500)))

      running(app) {
        val result = await(connector.getClaimInformation("NDRC-1234", Security))
        result shouldBe None
      }
    }
  }

  trait Setup {
    val mockHttp: HttpClient = mock[HttpClient]
    val mockClaimCache: ClaimsCache = mock[ClaimsCache]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val specificClaimResponse: SpecificClaimResponse = SpecificClaimResponse(
      "Pending",
      "NDRC-1234",
      Some("someEORI")
    )

    val allClaimsResponse: AllClaimsResponse = AllClaimsResponse(
      Seq(
        AllClaimsDetail("NDRC-1234", C285, "In Progress", "someEori1", "someEori2", Some("someEori3"), Some("1000"), Some("1000")),
        AllClaimsDetail("SCTY-2345", Security, "Closed", "someEori1", "someEori2", Some("someEori3"), Some("1000"), Some("1000")),
        AllClaimsDetail("NDRC-6789", C285, "Pending", "someEori1", "someEori2", Some("someEori3"), Some("1000"), Some("1000")),
      )
    )

    val app: Application = GuiceApplicationBuilder().overrides(
      inject.bind[HttpClient].toInstance(mockHttp),
      inject.bind[ClaimsCache].toInstance(mockClaimCache)
    ).configure(
      "play.filters.csp.nonce.enabled" -> "false",
      "auditing.enabled" -> "false",
      "metrics.enabled" -> "false"
    ).build()

    val connector: FinancialsApiConnector = app.injector.instanceOf[FinancialsApiConnector]
  }

}
