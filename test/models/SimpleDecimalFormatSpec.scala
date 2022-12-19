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

import models.SimpleDecimalFormat
import models.Nonce
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.{JsResult, JsString}
import utils.SpecBase

class SimpleDecimalFormatSpec extends SpecBase {

  "SimpleDecimalFormat" should {
    "return an error when a JsNumber not passed" in {
      val json                    = JsString("invalid")
      val result: JsResult[Nonce] =
        SimpleDecimalFormat[Nonce](s => Nonce(s.toIntExact), n => BigDecimal(n.value)).reads(json)
      result.isError mustBe true
    }
  }

  "Nonce" should {
    "equals should return correct values" in {
      val nonce = Nonce(111)
      nonce == Nonce(123) shouldBe false
      nonce == Nonce(111) shouldBe true
      nonce == "testing"  shouldBe false

    }
  }
}
