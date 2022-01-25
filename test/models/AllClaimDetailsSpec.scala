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

import models.responses.AllClaimsDetail
import utils.SpecBase

import java.time.LocalDate

class AllClaimDetailsSpec extends SpecBase {

  "toClaim" should {
    "return InProgressClaim when case status is 'In Progress'" in new Setup {
      createAllDetailsClaim("In Progress").toClaim shouldBe InProgressClaim("NDRC-0001", C285, LocalDate.of(9999, 1, 1))
    }

    "return PendingClaim when case status is 'Pending'" in new Setup {
      createAllDetailsClaim("Pending").toClaim shouldBe PendingClaim("NDRC-0001", C285, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 1, 1))
    }

    "return ClosedClaim when case status is 'Closed'" in new Setup {
      createAllDetailsClaim("Closed").toClaim shouldBe ClosedClaim("NDRC-0001", C285, LocalDate.of(9999, 1, 1), LocalDate.of(9999, 2, 1))
    }

    "throw an exception when unknown claim type passed" in new Setup {
      intercept[RuntimeException] {
        createAllDetailsClaim("Unknown").toClaim
      }.getMessage shouldBe "Unknown Case Status: Unknown"
    }
  }

  trait Setup {
    def createAllDetailsClaim(status: String): AllClaimsDetail =
      AllClaimsDetail(
        "NDRC-0001",
        C285,
        status,
        "someEori",
        "someEori1",
        None,
        None,
        None
      )
  }

}
