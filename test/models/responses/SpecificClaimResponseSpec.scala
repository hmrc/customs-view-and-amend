/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import utils.SpecBase

class SpecificClaimResponseSpec extends SpecBase {

  "SpecificClaimResponse" should {
    "serialize and deserialize" in new SetupBase {

      val specificClaimResponse =
        SpecificClaimResponse(
          "SCTY",
          CDFPayCaseFound = true,
          None,
          Some(sctyCase)
        )

      val json: JsValue = Json.toJson(specificClaimResponse)
      json shouldBe Json.obj(
        "CDFPayService"   -> "SCTY",
        "CDFPayCaseFound" -> true,
        "SCTYCase"        -> Json.obj(
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
      )

      val deserialized: SpecificClaimResponse = json.as[SpecificClaimResponse]
      deserialized shouldBe specificClaimResponse
    }
  }
}
