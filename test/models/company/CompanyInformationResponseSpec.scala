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

package models.company

import play.api.libs.json.Json
import utils.SpecBase

class CompanyInformationResponseSpec extends SpecBase {

  "CompanyInformationResponse" should {
    "serialize and deserialize" in {
      val address                    = CompanyAddress(
        streetAndNumber = "1 test lane",
        city = "London",
        postalCode = Some("AA1 YXX"),
        countryCode = "GB"
      )
      val companyInformationResponse = CompanyInformationResponse(name = "name", address = address)
      val json                       = Json.toJson(companyInformationResponse)
      json shouldBe Json.obj(
        "name"    -> "name",
        "address" -> Json.obj(
          "streetAndNumber" -> "1 test lane",
          "city"            -> "London",
          "postalCode"      -> "AA1 YXX",
          "countryCode"     -> "GB"
        )
      )

      val deserialized = json.as[CompanyInformationResponse]
      deserialized shouldBe companyInformationResponse
    }
  }
}
