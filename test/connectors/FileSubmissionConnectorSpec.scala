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

import connector.FileSubmissionConnector
import models.FileSelection.AdditionalSupportingDocuments
import models._
import models.file_upload.UploadedFile
import models.responses.{AllClaimsResponse, Claims, NDRCCaseDetails, SCTYCaseDetails, SpecificClaimResponse}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.{Application, inject}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class FileSubmissionConnectorSpec extends SpecBase {

  "fileUpload" should {
    "return 'true' if the upload was successful" in new Setup {
      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(ACCEPTED, "")))

      running(app) {
        val result = await(
          connector.submitFileToCDFPay(
            "declarationId",
            false,
            "eori",
            NDRC,
            "caseNumber",
            Seq(
              UploadedFile(
                "ref",
                "/uri",
                "timestamp",
                "sum",
                "file",
                "mime",
                10,
                None,
                AdditionalSupportingDocuments,
                None
              )
            )
          )
        )
        result shouldBe true
      }
    }

    "return 'false' if the status returned was not ACCEPTED" in new Setup {
      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      running(app) {
        val result = await(connector.submitFileToCDFPay("declarationId", false, "eori", NDRC, "caseNumber", Seq.empty))
        result shouldBe false
      }
    }

    "return false on an exception from the API" in new Setup {
      when[Future[HttpResponse]](mockHttp.POST(any, any, any)(any, any, any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("", 500, 500)))

      running(app) {
        val result = await(connector.submitFileToCDFPay("declarationId", false, "eori", NDRC, "caseNumber", Seq.empty))
        result shouldBe false
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
              Some("20210321"),
              Some("20211220"),
              "ACS",
              "Pending",
              None,
              "GB744638982000",
              Some("GB744638982000"),
              Some("GB744638982000"),
              Some("12000.56"),
              Some("3412.01"),
              Some("broomer007")
            ),
            SCTYCaseDetails(
              "SEC-2107",
              "21LLLLLLLLLL12343",
              Some("20210321"),
              Some("20211220"),
              "ACS",
              "Closed",
              Some("Closed"),
              "GB744638982000",
              Some("GB744638982000"),
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

    val connector: FileSubmissionConnector = app.injector.instanceOf[FileSubmissionConnector]
  }

}
