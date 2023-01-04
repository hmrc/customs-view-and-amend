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

package models

import models.CaseType._
import play.api.libs.json.{JsString, JsSuccess}
import utils.SpecBase

class CaseTypeSpec extends SpecBase {

  "format" should {
    "correctly format data in read and writes" in {
      CaseType.format.writes(Individual) shouldBe JsString("Individual")
      CaseType.format.writes(Bulk) shouldBe JsString("Bulk")
      CaseType.format.writes(CMA) shouldBe JsString("CMA")
      CaseType.format.writes(C18) shouldBe JsString("C18")
      CaseType.format.reads(JsString("Individual")) shouldBe JsSuccess(Individual)
      CaseType.format.reads(JsString("Bulk")) shouldBe JsSuccess(Bulk)
      CaseType.format.reads(JsString("CMA")) shouldBe JsSuccess(CMA)
      CaseType.format.reads(JsString("C18")) shouldBe JsSuccess(C18)
    }
  }
}
