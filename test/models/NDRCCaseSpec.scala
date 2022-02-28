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

import models.responses.NDRCCase
import play.api.libs.json.Json
import utils.SpecBase

class NDRCCaseSpec extends SpecBase {

  "toClaimDetail" should {
    "transform the case status into the corresponding case object" in {
      val inProgressCase = ndrcCase.copy(ndrcCase.NDRCDetail.copy(caseStatus = "In Progress", MRNDetails = None, entryDetails = None))
      val pendingCase = ndrcCase.copy(ndrcCase.NDRCDetail.copy(caseStatus = "Pending"))
      val closedCase = ndrcCase.copy(ndrcCase.NDRCDetail.copy(caseStatus = "Closed"))

      inProgressCase.toClaimDetail(None).claimStatus shouldBe InProgress
      pendingCase.toClaimDetail(None).claimStatus shouldBe Pending
      closedCase.toClaimDetail(None).claimStatus shouldBe Closed
    }

    "read/write json correctly" in {

      val json =
        """{
          |    "claimantEORI": "ClaimaintEori",
          |    "declarationID": "DeclarationId",
          |    "caseStatus": "Closed",
          |    "descOfRejectedGoods": "description of rejected goods",
          |    "claimantEmailAddress": "email@email.com",
          |    "CDFPayCaseNumber": "CaseNumber",
          |    "entryDetails":
          |    [
          |        {
          |            "entryNumber": "entryNumber",
          |            "mainDeclarationReference": true
          |        }
          |    ],
          |    "caseType": "NDRC",
          |    "importerEORI": "SomeOtherEori",
          |    "claimType": "C285",
          |    "closedDate": "20221112",
          |    "descOfGoods": "description of goods",
          |    "MRNDetails":
          |    [
          |        {
          |            "MRNNumber": "MRN",
          |            "mainDeclarationReference": true
          |        }
          |    ],
          |    "declarantEORI": "SomeEori",
          |    "claimantName": "name",
          |    "claimStartDate": "20221012",
          |    "basisOfClaim": "basis of claim",
          |    "reimbursement":
          |    [
          |        {
          |            "reimbursementDate": "date",
          |            "reimbursementAmount": "10.00",
          |            "taxType": "10.00",
          |            "reimbursementMethod": "method"
          |        }
          |    ],
          |    "totalExciseRefundAmount": "600000",
          |    "totalVATRefundAmount": "600000",
          |    "totalCustomsRefundAmount": "600000",
          |    "totalCustomsClaimAmount": "600000",
          |    "totalReimbursmentAmount": "600000",
          |    "totalVATClaimAmount": "600000",
          |    "totalClaimAmount": "600000",
          |    "totalExciseClaimAmount": "600000",
          |    "totalRefundAmount": "600000"
          |}""".stripMargin


      Json.parse(json).as[NDRCCase] shouldBe ndrcCase
      Json.toJson(ndrcCase) shouldBe Json.parse(json)
    }

    "throw an exception if an invalid case status passed from the api" in {
      val invalidCase = ndrcCase.copy(ndrcCase.NDRCDetail.copy(caseStatus = "INVALID"))

      intercept[RuntimeException]{
        invalidCase.toClaimDetail(None)
      }.getMessage shouldBe "Unknown case status: INVALID"
    }
  }

}
