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
import org.scalatest.Inside
import play.api.i18n.{DefaultMessagesApi, Lang, Messages}
import utils.SpecBase

import java.time.LocalDate

class AllClaimDetailsSpec extends SpecBase with Inside {

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

  "ClaimDetail" should {
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

  "AllClaims" should {
    "find claim by caseNumber" in {
      inside(allClaims.findByCaseNumber("SCTY-1099")) { case Some(c: ClosedClaim) =>
        c.caseNumber shouldBe "SCTY-1099"
      }
      inside(allClaims.findByCaseNumber("NDRC-2021")) { case Some(c: PendingClaim) =>
        c.caseNumber shouldBe "NDRC-2021"
      }
      inside(allClaims.findByCaseNumber("NDRC-3078")) { case Some(c: InProgressClaim) =>
        c.caseNumber shouldBe "NDRC-3078"
      }
    }
    "search claim by query" in {
      allClaims.searchForClaim("NDRC-3033") should have size 1
      allClaims.searchForClaim("NDRC-2001") should have size 1
      allClaims.searchForClaim("SCTY-1099") should have size 1
      allClaims.searchForClaim("NDRC-1099") should have size 0
      allClaims.searchForClaim("NDRC-4000") should have size 0
      allClaims.searchForClaim("")          should have size 0
      allClaims.searchForClaim("foo")       should have size 0
    }
    "check claims not empty" in {
      allClaims.nonEmpty                                                               shouldBe true
      allClaims.copy(pendingClaims = Seq.empty).nonEmpty                               shouldBe true
      allClaims.copy(pendingClaims = Seq.empty, inProgressClaims = Seq.empty).nonEmpty shouldBe true
      allClaims
        .copy(pendingClaims = Seq.empty, inProgressClaims = Seq.empty, closedClaims = Seq.empty)
        .nonEmpty                                                                      shouldBe false
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

    def createNdrcDetailsClaim(status: String): NDRCCaseDetails =
      NDRCCaseDetails(
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
    securityGoodsDescription = None,
    caseType = Some(CaseType.Individual)
  )

  val closedClaims: Seq[ClosedClaim]        = (1 to 100).map { value =>
    ClosedClaim(
      "MRN",
      s"SCTY-${1000 + value}",
      SCTY,
      None,
      LocalDate.of(2021, 2, 1).plusDays(value),
      LocalDate.of(2022, 1, 1).plusDays(value),
      "Closed"
    )
  }
  val pendingClaims: Seq[PendingClaim]      = (1 to 100).map { value =>
    PendingClaim(
      "MRN",
      s"NDRC-${2000 + value}",
      NDRC,
      None,
      LocalDate.of(2021, 2, 1).plusDays(value),
      LocalDate.of(2022, 1, 1).plusDays(value)
    )
  }
  val inProgressClaim: Seq[InProgressClaim] = (1 to 100).map { value =>
    InProgressClaim("MRN", s"NDRC-${3000 + value}", NDRC, None, LocalDate.of(2021, 2, 1).plusDays(value))
  }

  val allClaims: AllClaims = AllClaims(
    pendingClaims = pendingClaims,
    inProgressClaims = inProgressClaim,
    closedClaims = closedClaims
  )
}
