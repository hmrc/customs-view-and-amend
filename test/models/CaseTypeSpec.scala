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

import play.api.libs.json.{JsString, JsSuccess}
import utils.SpecBase

class CaseTypeSpec extends SpecBase {

  "format" should {
    "correctly format data in read and writes" in {
      CaseType.format.writes(Single) shouldBe JsString("Individual")
      CaseType.format.writes(Multiple) shouldBe JsString("Bulk")
      CaseType.format.writes(CMA) shouldBe JsString("CMA")
      CaseType.format.writes(C18) shouldBe JsString("C18")
      CaseType.format.reads(JsString("Individual")) shouldBe JsSuccess(Single)
      CaseType.format.reads(JsString("Bulk")) shouldBe JsSuccess(Multiple)
      CaseType.format.reads(JsString("CMA")) shouldBe JsSuccess(CMA)
      CaseType.format.reads(JsString("C18")) shouldBe JsSuccess(C18)
      CaseType.format.reads(JsString("Unknown")).isError shouldBe true
      CaseType.pathBindable.bind("something", "Individual") shouldBe Right(Single)
      CaseType.pathBindable.bind("something", "Bulk") shouldBe Right(Multiple)
      CaseType.pathBindable.bind("something", "CMA") shouldBe Right(CMA)
      CaseType.pathBindable.bind("something", "C18") shouldBe Right(C18)
      CaseType.pathBindable.bind("something", "Unknown") shouldBe Left("Invalid caseType")
      CaseType.pathBindable.unbind("something", Single) shouldBe "Individual"
      CaseType.pathBindable.unbind("something", Multiple) shouldBe "Bulk"
      CaseType.pathBindable.unbind("something", CMA) shouldBe "CMA"
      CaseType.pathBindable.unbind("something", C18) shouldBe "C18"
    }
  }
}
