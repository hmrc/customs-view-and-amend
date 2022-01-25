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

import models.responses.SpecificClaimResponse
import utils.SpecBase

class SpecificClaimResponseSpec extends SpecBase {

  "transformCaseStatus" should {
    "return InProgress when case status is 'In Progress'" in new Setup {
      createSpecificClaimResponse("In Progress").toClaimDetail(C285).claimStatus shouldBe InProgress
    }

    "return Closed when case status is 'Closed'" in new Setup {
      createSpecificClaimResponse("Closed").toClaimDetail(C285).claimStatus shouldBe Closed
    }

    "return Pending when case status is 'Pending'" in new Setup {
      createSpecificClaimResponse("Pending").toClaimDetail(C285).claimStatus shouldBe Pending
    }

    "throw an exception when an unknown case status passed" in new Setup {
      intercept[RuntimeException] {
        createSpecificClaimResponse("Unknown").toClaimDetail(C285).claimStatus
      }.getMessage shouldBe "Unknown case status: Unknown"
    }
  }


  trait Setup {
    def createSpecificClaimResponse(status: String): SpecificClaimResponse = SpecificClaimResponse(
      status,
      "NDRC-1234",
      Some("someEORI")
    )
  }

}
