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

package models.responses

import models.{Closed, Pending, InProgress}
import play.api.libs.json.{JsValue, Json}
import utils.SpecBase

class SCTYCaseSpec extends SpecBase {

  "SCTYCase" should {
    "transform the caseStatus to the correct object" in new SetupBase {
      val closedCase: SCTYCase     = sctyCase.copy(caseStatus = "Closed")
      val pendingCase: SCTYCase    = sctyCase.copy(caseStatus = "Pending")
      val inProgressCase: SCTYCase = sctyCase.copy(caseStatus = "In Progress")

      closedCase.toClaimDetail(None).claimStatus     shouldBe Closed
      pendingCase.toClaimDetail(None).claimStatus    shouldBe Pending
      inProgressCase.toClaimDetail(None).claimStatus shouldBe InProgress
    }

    "throw an exception on an invalid case status" in new SetupBase {
      val invalid: SCTYCase = sctyCase.copy(caseStatus = "INVALID")
      intercept[RuntimeException] {
        invalid.toClaimDetail(None)
      }.getMessage shouldBe "Unknown case status: INVALID"
    }

    "serialize and deserialize" in new SetupBase {

      val json: JsValue = Json.toJson(sctyCase)

      json shouldBe Json.obj(
        "CDFPayCaseNumber"         -> "caseNumber",
        "declarationID"            -> "declarationId",
        "reasonForSecurity"        -> "Reason for security",
        "procedureCode"            -> "Procedure Code",
        "caseStatus"               -> "Closed",
        "caseSubStatus"            -> "Refused",
        "goods"                    -> Json.arr(
          Json.obj(
            "itemNumber"       -> "itemNumber",
            "goodsDescription" -> "description"
          )
        ),
        "declarantEORI"            -> "someEori",
        "importerEORI"             -> "someOtherEori",
        "claimantEORI"             -> "claimantEori",
        "totalCustomsClaimAmount"  -> "600000",
        "totalVATClaimAmount"      -> "600000",
        "totalClaimAmount"         -> "600000",
        "totalReimbursementAmount" -> "600000",
        "claimStartDate"           -> "20221210",
        "claimantName"             -> "name",
        "claimantEmailAddress"     -> "email@email.com",
        "closedDate"               -> "20221012",
        "reimbursement"            -> Json.arr(
          Json.obj(
            "reimbursementDate"   -> "date",
            "reimbursementAmount" -> "10.00",
            "taxType"             -> "10.00",
            "reimbursementMethod" -> "method"
          )
        )
      )
    }
  }
}
