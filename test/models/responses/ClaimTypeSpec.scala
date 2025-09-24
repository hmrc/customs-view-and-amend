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

import play.api.libs.json.{JsString, JsSuccess}
import utils.SpecBase

class ClaimTypeSpec extends SpecBase {

  "format" should {
    "correctly format data in read and writes" in {
      ClaimType.format.writes(C285)                                                 shouldBe JsString("C285")
      ClaimType.format.writes(`C&E1179`)                                            shouldBe JsString("C&E1179")
      ClaimType.format.reads(JsString("C&E1179"))                                   shouldBe JsSuccess(`C&E1179`)
      ClaimType.format.reads(JsString("C285"))                                      shouldBe JsSuccess(C285)
      ClaimType.format.reads(JsString("Unknown")).isError                           shouldBe true
      ClaimType.pathBindable.bind("something", "C285")                              shouldBe Right(C285)
      ClaimType.pathBindable.bind("something", "CE1179")                            shouldBe Right(`C&E1179`)
      ClaimType.pathBindable.bind("something", "Unknown")                           shouldBe Left("Invalid service type")
      ClaimType.pathBindable.unbind("something", C285)                              shouldBe "C285"
      ClaimType.pathBindable.unbind("something", `C&E1179`)                         shouldBe "CE1179"
      ClaimType.queryBindable.bind("claimType", Map(("claimType", Seq("C285"))))    shouldBe Some(Right(C285))
      ClaimType.queryBindable.bind("claimType", Map(("claimType", Seq("CE1179"))))  shouldBe Some(Right(`C&E1179`))
      ClaimType.queryBindable.bind("claimType", Map(("claimType", Seq("Unknown")))) shouldBe Some(
        Left("Invalid service type")
      )
      ClaimType.queryBindable.unbind("claimType", C285)                             shouldBe "claimType=C285"
      ClaimType.queryBindable.unbind("claimType", `C&E1179`)                        shouldBe "claimType=CE1179"
    }
  }

}
