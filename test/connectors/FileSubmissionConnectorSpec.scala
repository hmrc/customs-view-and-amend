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
import models.*
import models.FileSelection.AdditionalSupportingDocuments
import models.file_upload.{Dec64UploadedFile, UploadedFile}
import models.responses.{AllClaimsResponse, Claims, NDRCCaseDetails, SCTYCaseDetails, SpecificClaimResponse}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.inject
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import utils.SpecBase
import java.time.LocalDate

class FileSubmissionConnectorSpec extends SpecBase with HttpV2Support {

  "fileUpload" should {
    "return 'true' if the upload was successful" in new Setup {

      val request = Dec64UploadRequest(
        id = id,
        declarationId = declarationId,
        eori = eori,
        entryNumber = entryNumber,
        applicationName = ndrc.dec64ServiceType,
        caseNumber = caseNumber,
        uploadedFiles = uploadedFiles.map(_.toDec64UploadedFile),
        reasonForSecurity = reasonForSecurity
      )

      mockHttpPost[HttpResponse]("http://host1:123/cds-reimbursement-claim/claims/files", Json.toJson(request))(
        HttpResponse(ACCEPTED, "")
      )

      running(app) {
        val result = await(
          connector.submitFileToCDFPay(
            declarationId = declarationId,
            entryNumber = entryNumber,
            eori = eori,
            serviceType = ndrc,
            caseNumber = caseNumber,
            files = uploadedFiles,
            reasonForSecurity = reasonForSecurity,
            id = id
          )
        )
        result shouldBe true
      }
    }

    "return 'false' if the status returned was not ACCEPTED" in new Setup {

      val request = Dec64UploadRequest(
        id = id,
        declarationId = declarationId,
        eori = eori,
        entryNumber = entryNumber,
        applicationName = ndrc.dec64ServiceType,
        caseNumber = caseNumber,
        uploadedFiles = emptyUploadedFiles.map(_.toDec64UploadedFile),
        reasonForSecurity = reasonForSecurity
      )

      mockHttpPost[HttpResponse]("http://host1:123/cds-reimbursement-claim/claims/files", Json.toJson(request))(
        HttpResponse(NO_CONTENT, "")
      )

      running(app) {
        val result =
          await(
            connector.submitFileToCDFPay(
              declarationId = declarationId,
              entryNumber = entryNumber,
              eori = eori,
              serviceType = ndrc,
              caseNumber = caseNumber,
              files = emptyUploadedFiles,
              reasonForSecurity = reasonForSecurity,
              id = id
            )
          )
        result shouldBe false
      }
    }

    "return false on an exception from the API" in new Setup {
      val request = Dec64UploadRequest(
        id = id,
        declarationId = declarationId,
        eori = eori,
        entryNumber = entryNumber,
        applicationName = ndrc.dec64ServiceType,
        caseNumber = caseNumber,
        uploadedFiles = emptyUploadedFiles.map(_.toDec64UploadedFile),
        reasonForSecurity = reasonForSecurity
      )

      mockHttpPostWithException("http://host1:123/cds-reimbursement-claim/claims/files", Json.toJson(request))(
        UpstreamErrorResponse("", 500, 500)
      )

      running(app) {
        val result =
          await(
            connector.submitFileToCDFPay(
              declarationId = "declarationId",
              entryNumber = false,
              eori = "eori",
              serviceType = NDRC,
              caseNumber = "caseNumber",
              files = Seq.empty,
              reasonForSecurity = None,
              id = id
            )
          )
        result shouldBe false
      }
    }
  }

  trait Setup extends SetupBase {

    val specificClaimResponse: SpecificClaimResponse = SpecificClaimResponse(
      "NDRC",
      CDFPayCaseFound = true,
      Some(ndrcCase),
      None
    )

    val startDate: LocalDate = LocalDate.of(2021, 3, 21)

    val id                                    = "testId"
    val declarationId                         = "declarationId"
    val entryNumber                           = false
    val eori                                  = "eori"
    val ndrc                                  = NDRC
    val caseNumber                            = "caseNumber"
    val uploadedFiles: Seq[UploadedFile]      = Seq(
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
    val emptyUploadedFiles: Seq[UploadedFile] = Seq.empty
    val reasonForSecurity                     = None

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

    val app = GuiceApplicationBuilder()
      .overrides(
        inject.bind[HttpClientV2].toInstance(mockHttp)
      )
      .configure(
        "play.filters.csp.nonce.enabled"                     -> "false",
        "auditing.enabled"                                   -> "false",
        "metrics.enabled"                                    -> "false",
        "microservice.services.cds-reimbursement-claim.host" -> "host1",
        "microservice.services.cds-reimbursement-claim.port" -> "123"
      )
      .build()

    val connector: FileSubmissionConnector = app.injector.instanceOf[FileSubmissionConnector]
  }

}
