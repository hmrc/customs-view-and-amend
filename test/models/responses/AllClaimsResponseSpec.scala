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

class AllClaimsResponseSpec extends SpecBase {

  private val claims = Claims(
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

  "AllClaimsResponse" should {
    "serialize and deserialize" in new SpecBase {
      val allClaimsResponse: AllClaimsResponse = AllClaimsResponse(claims = claims)

      val json: JsValue = Json.toJson(allClaimsResponse)
      json shouldBe Json.obj(
        "claims" -> Json.obj(
          "sctyClaims" -> Json.arr(
            Json.obj(
              "CDFPayCaseNumber"         -> "SEC-2108",
              "declarationID"            -> "21LLLLLLLLLL12344",
              "claimStartDate"           -> "20210321",
              "closedDate"               -> "20211220",
              "reasonForSecurity"        -> "ACS",
              "caseStatus"               -> "Pending",
              "declarantEORI"            -> "GB744638982000",
              "importerEORI"             -> "GB744638982000",
              "claimantEORI"             -> "GB744638982000",
              "totalCustomsClaimAmount"  -> "12000.56",
              "totalVATClaimAmount"      -> "3412.01",
              "declarantReferenceNumber" -> "broomer007"
            ),
            Json.obj(
              "CDFPayCaseNumber"         -> "SEC-2107",
              "declarationID"            -> "21LLLLLLLLLL12343",
              "claimStartDate"           -> "20210321",
              "closedDate"               -> "20211220",
              "reasonForSecurity"        -> "ACS",
              "caseStatus"               -> "Closed",
              "caseSubStatus"            -> "Closed",
              "declarantEORI"            -> "GB744638982000",
              "importerEORI"             -> "GB744638982000",
              "claimantEORI"             -> "GB744638982000",
              "totalCustomsClaimAmount"  -> "12000.56",
              "totalVATClaimAmount"      -> "3412.01",
              "declarantReferenceNumber" -> "broomer007"
            )
          ),
          "ndrcClaims" -> Json.arr(
            Json.obj(
              "CDFPayCaseNumber"         -> "NDRC-2109",
              "declarationID"            -> "21LLLLLLLLLLLLLLL9",
              "claimStartDate"           -> "20210321",
              "closedDate"               -> "20211220",
              "caseStatus"               -> "In Progress",
              "declarantEORI"            -> "GB744638982000",
              "importerEORI"             -> "GB744638982000",
              "claimantEORI"             -> "GB744638982000",
              "totalCustomsClaimAmount"  -> "3000.20",
              "totalVATClaimAmount"      -> "784.66",
              "totalExciseClaimAmount"   -> "1200.00",
              "declarantReferenceNumber" -> "KWMREF1",
              "basisOfClaim"             -> "Duplicate Entry"
            )
          )
        )
      )

      val deserialized: AllClaimsResponse = json.as[AllClaimsResponse]
      deserialized shouldBe allClaimsResponse
    }
  }
}
