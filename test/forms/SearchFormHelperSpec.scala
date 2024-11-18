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

package forms

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.{Form, FormError}
import utils.SpecBase

class SearchFormHelperSpec extends SpecBase {
  "searchForm" should {

    val testForm = SearchFormHelper.form

    "fill with valid input" in {
      val result: Form[String] = testForm.fillAndValidate("Foo")
      result.get shouldBe "Foo"
    }

    "report error when an empty input" in {
      val result = testForm.fillAndValidate("")
      result.error("search") must contain(FormError("search", "claim-search.error.required"))
    }

    "bind valid input" in {
      val result = testForm.bind(Map("search" -> "Foo"))
      result.value mustBe Some("Foo")
    }

    "filter html injection" in {
      val result = testForm.bind(Map("search" -> "<img src='https://owasp.org/assets/images/logo.png'>Foo</img>"))
      result.value mustBe Some("Foo")
    }
  }
}
