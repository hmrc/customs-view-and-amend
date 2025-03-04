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

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import play.api.data.Form
import play.api.data.Forms.{mapping, text}

object SearchFormHelper {

  val form: Form[String] =
    Form(
      mapping(
        "search" ->
          text
            .transform(cleanInput, identity[String])
            .verifying(
              "claim-search.error.required",
              str => str.nonEmpty
            )
      )(identity)(Some(_))
    )

  def cleanInput(s: String): String =
    Jsoup.clean(s, Safelist.none())
}
