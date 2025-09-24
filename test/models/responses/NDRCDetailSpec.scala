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

class NDRCDetailSpec extends SpecBase {

  "NDRCDetail" should {
    "serialize and deserialize" in new SetupBase {

      val ndrcDetail: NDRCDetail = ndrcCase.NDRCDetail
      val json: JsValue          = Json.toJson(ndrcDetail)

      json shouldBe Json.obj(
        "CDFPayCaseNumber"     -> "CaseNumber",
        "declarationID"        -> "DeclarationId",
        "claimType"            -> "C285",
        "caseType"             -> "Individual",
        "caseStatus"           -> "Closed",
        "caseSubStatus"        -> "Refused",
        "descOfGoods"          -> "description of goods",
        "descOfRejectedGoods"  -> "description of rejected goods",
        "declarantEORI"        -> "SomeEori",
        "importerEORI"         -> "SomeOtherEori",
        "claimantEORI"         -> "ClaimaintEori",
        "basisOfClaim"         -> "basis of claim",
        "claimStartDate"       -> "20221012",
        "claimantName"         -> "name",
        "claimantEmailAddress" -> "email@email.com",
        "closedDate"           -> "20221112",
        "MRNDetails"           -> Json.arr(
          Json.obj(
            "MRNNumber"                -> "MRN",
            "mainDeclarationReference" -> true
          )
        ),
        "entryDetails"         -> Json.arr(
          Json.obj(
            "entryNumber"              -> "entryNumber",
            "mainDeclarationReference" -> true
          )
        ),
        "reimbursement"        -> Json.arr(
          Json.obj(
            "reimbursementDate"   -> "date",
            "reimbursementAmount" -> "10.00",
            "taxType"             -> "10.00",
            "reimbursementMethod" -> "method"
          )
        )
      )

      val deserialized: NDRCDetail = json.as[NDRCDetail]
      deserialized shouldBe ndrcDetail
    }
  }
}
