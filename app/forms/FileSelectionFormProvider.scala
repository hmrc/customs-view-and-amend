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

package forms

import models.responses.{C285, ClaimType, `C&E1179`}
import models.{Enumerable, FileSelection}
import play.api.data.Form

class FileSelectionFormProvider(claimType: ClaimType) extends Mappings {

  implicit val enumerable: Enumerable[FileSelection] =
    FileSelection.enumerable

  def apply(): Form[FileSelection] =
    Form(
      "value" -> enumerable[FileSelection]("file.selection.error.required")
    )
}
