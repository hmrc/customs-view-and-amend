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

package models

import models.responses.{NDRCCaseDetails, ProcedureDetail, SCTYCaseDetails, `C&E1179`}
import play.api.i18n.{DefaultMessagesApi, Lang, Messages}
import utils.SpecBase

import java.time.LocalDate

class AllClaimDetailsSpec extends SpecBase {

  "toClaim for SCTY" should {
    "return InProgressClaim when case status is 'In Progress'" in new Setup {
      createSctyDetailsClaim("In Progress").toClaim shouldBe InProgressClaim(
        "21LLLLLLLLLL12345",
        "SEC-2109",
        SCTY,
        Some("broomer007"),
        startDate
      )
    }

    "return PendingClaim when case status is 'Pending'" in new Setup {
      createSctyDetailsClaim("Pending").toClaim shouldBe PendingClaim(
        "21LLLLLLLLLL12345",
        "SEC-2109",
        SCTY,
        Some("broomer007"),
        startDate,
        startDate.plusDays(30)
      )
    }

    "return ClosedClaim when case status is 'Closed'" in new Setup {
      createSctyDetailsClaim("Closed").toClaim shouldBe ClosedClaim(
        "21LLLLLLLLLL12345",
        "SEC-2109",
        SCTY,
        Some("broomer007"),
        startDate,
        endDate,
        "Closed"
      )
    }

    "throw an exception when unknown claim type passed" in new Setup {
      intercept[RuntimeException] {
        createSctyDetailsClaim("Unknown").toClaim
      }.getMessage shouldBe "Unknown Case Status: Unknown"
    }
  }

  "toClaim for NDRC" should {
    "return InProgressClaim when case status is 'In Progress'" in new Setup {
      createNdrcDetailsClaim("In Progress").toClaim shouldBe InProgressClaim(
        "21LLLLLLLLLLLLLLL9",
        "NDRC-2109",
        NDRC,
        Some("KWMREF1"),
        startDate
      )
    }

    "return PendingClaim when case status is 'Pending'" in new Setup {
      createNdrcDetailsClaim("Pending").toClaim shouldBe PendingClaim(
        "21LLLLLLLLLLLLLLL9",
        "NDRC-2109",
        NDRC,
        Some("KWMREF1"),
        startDate,
        startDate.plusDays(30)
      )
    }

    "return ClosedClaim when case status is 'Closed'" in new Setup {
      createNdrcDetailsClaim("Closed").toClaim shouldBe ClosedClaim(
        "21LLLLLLLLLLLLLLL9",
        "NDRC-2109",
        NDRC,
        Some("KWMREF1"),
        startDate,
        endDate,
        "Closed"
      )
    }

    "throw an exception when unknown claim type passed" in new Setup {
      intercept[RuntimeException] {
        createNdrcDetailsClaim("Unknown").toClaim
      }.getMessage shouldBe "Unknown Case Status: Unknown"
    }
  }

  "claimDetail" should {
    "format startDate" in {
      implicit val messages: Messages = new DefaultMessagesApi(
        Map("en" -> Map("month.12" -> "Foo"))
      ).preferred(Seq(Lang("en")))

      claimDetail.formattedStartDate()(messages) shouldBe "14 Foo 2022"
    }

    "format closedDate" in {
      implicit val messages: Messages = new DefaultMessagesApi(
        Map("en" -> Map("month.7" -> "Bar"))
      ).preferred(Seq(Lang("en")))

      claimDetail.formattedClosedDate()(messages) shouldBe Some("23 Bar 2021")
    }

    "check isEntryNumber" in {
      claimDetail.isEntryNumber shouldBe false
    }

    "check multipleDeclarations" in {
      claimDetail.multipleDeclarations shouldBe false
    }
  }

  trait Setup {

    val startDate: LocalDate = LocalDate.of(2021, 3, 20)
    val endDate: LocalDate   = LocalDate.of(2021, 5, 20)

    def createSctyDetailsClaim(status: String): SCTYCaseDetails =
      SCTYCaseDetails(
        CDFPayCaseNumber = "SEC-2109",
        declarationID = "21LLLLLLLLLL12345",
        claimStartDate = "20210320",
        closedDate = Some("20210520"),
        reasonForSecurity = "ACS",
        caseStatus = status,
        caseSubStatus = Option(status),
        declarantEORI = "GB744638982000",
        importerEORI = "GB744638982000",
        claimantEORI = Some("GB744638982000"),
        totalCustomsClaimAmount = Some("12000.56"),
        totalVATClaimAmount = Some("3412.01"),
        declarantReferenceNumber = Some("broomer007")
      )

    def createNdrcDetailsClaim(status: String): NDRCCaseDetails = NDRCCaseDetails(
      CDFPayCaseNumber = "NDRC-2109",
      declarationID = "21LLLLLLLLLLLLLLL9",
      claimStartDate = "20210320",
      closedDate = Some("20210520"),
      caseStatus = status,
      caseSubStatus = Option(status),
      declarantEORI = "GB744638982000",
      importerEORI = "GB744638982000",
      claimantEORI = Some("GB744638982000"),
      totalCustomsClaimAmount = Some("3000.20"),
      totalVATClaimAmount = Some("784.66"),
      totalExciseClaimAmount = Some("1200.00"),
      declarantReferenceNumber = Some("KWMREF1"),
      basisOfClaim = Some("Duplicate Entry")
    )
  }

  val claimDetail: ClaimDetail = ClaimDetail(
    caseNumber = "NDRC-2109",
    serviceType = NDRC,
    declarationId = "21LLLLLLLLLLLLLLL9",
    mrn = Seq(ProcedureDetail("21LLLLLLLLLLLLLLL9", true)),
    entryNumbers = Seq.empty,
    lrn = None,
    claimantsEori = Some("GB744638982000"),
    claimStatus = Pending,
    caseSubStatus = None,
    claimType = Some(`C&E1179`),
    claimStartDate = LocalDate.of(2022, 12, 14),
    claimClosedDate = Some(LocalDate.of(2021, 7, 23)),
    totalClaimAmount = None,
    claimantsName = None,
    claimantsEmail = None,
    reasonForSecurity = None,
    securityGoodsDescription = None
  )
}
