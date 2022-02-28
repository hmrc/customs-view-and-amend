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

import utils.SpecBase

class SCTYCaseSpec extends SpecBase {

  "SCTYCase.toClaimDetail" should {
    "transform the caseStatus to the correct object" in {
      val closedCase = sctyCase.copy(caseStatus = "Closed")
      val pendingCase = sctyCase.copy(caseStatus = "Pending")
      val inProgressCase = sctyCase.copy(caseStatus = "In Progress")

      closedCase.toClaimDetail(None).claimStatus shouldBe Closed
      pendingCase.toClaimDetail(None).claimStatus shouldBe Pending
      inProgressCase.toClaimDetail(None).claimStatus shouldBe InProgress
    }

    "throw an exception on an invalid case status" in {
      val invalid = sctyCase.copy(caseStatus = "INVALID")
      intercept[RuntimeException] {
        invalid.toClaimDetail(None)
      }.getMessage shouldBe "Unknown case status: INVALID"

    }

  }

}
