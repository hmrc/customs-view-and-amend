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

package connectors

import connector.ClaimsConnector
import models._
import models.responses.{AllClaimsResponse, C285, Claims, NDRCCaseDetails, ProcedureDetail, SCTYCaseDetails, SpecificClaimResponse}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, inject}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimsConnectorSpec extends SpecBase {

  "getAllClaims" should {
    "return AllClaims and call the financials api" in new Setup {
      when[Future[AllClaimsResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(allClaimsResponse))

      running(app) {
        val result = await(connector.getAllClaims)
        result.closedClaims     shouldBe Seq(
          ClosedClaim(
            "21LLLLLLLLLL12343",
            "SEC-2107",
            SCTY,
            Some("broomer007"),
            startDate,
            LocalDate.of(2021, 12, 20),
            "Closed"
          )
        )
        result.inProgressClaims shouldBe Seq(
          InProgressClaim("21LLLLLLLLLLLLLLL9", "NDRC-2109", NDRC, Some("KWMREF1"), startDate)
        )
        result.pendingClaims    shouldBe Seq(
          PendingClaim("21LLLLLLLLLL12344", "SEC-2108", SCTY, Some("broomer007"), startDate, startDate.plusDays(30))
        )
      }
    }
  }

  "getClaimInformation" should {
    "return NDRC ClaimDetail when a data returned from the API" in new Setup {
      when[Future[SpecificClaimResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(specificClaimResponse))

      running(app) {
        val result = await(connector.getClaimInformation("NDRC-1234", NDRC, None)).value
        result.claimantsEori.value     shouldBe "ClaimaintEori"
        result.claimType.value         shouldBe C285
        result.mrn                     shouldBe Seq(ProcedureDetail("MRN", mainDeclarationReference = true))
        result.claimStatus             shouldBe Closed
        result.caseNumber              shouldBe "CaseNumber"
        result.claimStartDate.toString shouldBe "2022-10-12"
        result.claimantsEmail.value    shouldBe "email@email.com"
        result.claimantsName.value     shouldBe "name"
      }
    }

    "return SCTY ClaimDetail when a data returned from the API" in new Setup {
      val response: SpecificClaimResponse = SpecificClaimResponse(
        "SCTY",
        CDFPayCaseFound = true,
        None,
        Some(sctyCase)
      )

      when[Future[SpecificClaimResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(response))

      running(app) {
        val result = await(connector.getClaimInformation("SCTY-1234", NDRC, None)).value

        result.caseNumber           shouldBe "caseNumber"
        result.claimantsEmail.value shouldBe "email@email.com"
        result.claimantsName.value  shouldBe "name"
        result.declarationId        shouldBe "declarationId"
      }
    }

    "return None when NO_CONTENT returned from the API" in new Setup {
      when[Future[SpecificClaimResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 500, 500)))

      running(app) {
        val result = await(connector.getClaimInformation("NDRC-1234", NDRC, None))
        result shouldBe None
      }
    }

    "return None when both NDRC/SCTY claim returned for a single claim" in new Setup {
      val response: SpecificClaimResponse = SpecificClaimResponse(
        "NDRC",
        CDFPayCaseFound = true,
        Some(ndrcCase),
        Some(sctyCase)
      )

      when[Future[SpecificClaimResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(response))

      running(app) {
        val result = await(connector.getClaimInformation("NDRC-1234", NDRC, None))
        result shouldBe None
      }
    }

    "return None when an unexpected response returned from the API" in new Setup {
      val response: SpecificClaimResponse = SpecificClaimResponse(
        "NDRC",
        CDFPayCaseFound = true,
        None,
        None
      )

      when[Future[SpecificClaimResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(response))

      running(app) {
        val result = await(connector.getClaimInformation("NDRC-1234", NDRC, None))
        result shouldBe None
      }
    }
  }

  trait Setup extends SetupBase {
    val mockHttp: HttpClient       = mock[HttpClient]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val specificClaimResponse: SpecificClaimResponse = SpecificClaimResponse(
      "NDRC",
      CDFPayCaseFound = true,
      Some(ndrcCase),
      None
    )

    val startDate: LocalDate = LocalDate.of(2021, 3, 21)

    val allClaimsResponse: AllClaimsResponse =
      AllClaimsResponse(
        Claims(
          Seq(
            SCTYCaseDetails(
              "SEC-2108",
              "21LLLLLLLLLL12344",
              "20210321",
              Some("20211220"),
              "ACS",
              "Pending",
              None,
              "GB744638982000",
              "GB744638982000",
              Some("GB744638982000"),
              Some("12000.56"),
              Some("3412.01"),
              Some("broomer007")
            ),
            SCTYCaseDetails(
              "SEC-2107",
              "21LLLLLLLLLL12343",
              "20210321",
              Some("20211220"),
              "ACS",
              "Closed",
              Some("Closed"),
              "GB744638982000",
              "GB744638982000",
              Some("GB744638982000"),
              Some("12000.56"),
              Some("3412.01"),
              Some("broomer007")
            )
          ),
          Seq(
            NDRCCaseDetails(
              "NDRC-2109",
              "21LLLLLLLLLLLLLLL9",
              "20210321",
              Some("20211220"),
              "In Progress",
              None,
              "GB744638982000",
              "GB744638982000",
              Some("GB744638982000"),
              Some("3000.20"),
              Some("784.66"),
              Some("1200.00"),
              Some("KWMREF1"),
              Some("Duplicate Entry")
            )
          )
        )
      )

    val app: Application = GuiceApplicationBuilder()
      .overrides(
        inject.bind[HttpClient].toInstance(mockHttp)
      )
      .configure(
        "play.filters.csp.nonce.enabled" -> "false",
        "auditing.enabled"               -> "false",
        "metrics.enabled"                -> "false"
      )
      .build()

    val connector: ClaimsConnector = app.injector.instanceOf[ClaimsConnector]
  }

}
