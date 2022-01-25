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

import play.api.libs.json.{JsError, JsString, JsSuccess}
import utils.SpecBase

class ClaimTypeSpec extends SpecBase {

  "format" should {
    "correctly format data in read and writes" in {
      ClaimType.format.writes(C285) shouldBe JsString("NDRC")
      ClaimType.format.writes(Security) shouldBe JsString("SCTY")
      ClaimType.format.reads(JsString("SCTY")) shouldBe JsSuccess(Security)
      ClaimType.format.reads(JsString("NDRC")) shouldBe JsSuccess(C285)
      ClaimType.format.reads(JsString("Unknown")).isError shouldBe true
      ClaimType.pathBindable.bind("something", "NDRC") shouldBe Right(C285)
      ClaimType.pathBindable.bind("something", "SCTY") shouldBe Right(Security)
      ClaimType.pathBindable.bind("something", "Unknown") shouldBe Left("Invalid claim type")
    }
  }

}
