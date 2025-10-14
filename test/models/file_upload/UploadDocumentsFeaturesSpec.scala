/*
 * Copyright 2025 HM Revenue & Customs
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

package models.file_upload

import play.api.libs.json.Json
import utils.SpecBase

class UploadDocumentsFeaturesSpec extends SpecBase {

  "UploadDocumentsFeatures" should {
    "serialize and deserialize" in {
      val uploadDocumentsFeatures = UploadDocumentsFeatures(
        showUploadMultiple = Some(true),
        showYesNoQuestionBeforeContinue = Some(false),
        showAddAnotherDocumentButton = Some(false),
        showLanguageSelection = Some(true),
        enableMultipleFilesPicker = Some(true)
      )

      val json = Json.toJson(uploadDocumentsFeatures)
      json shouldBe Json.obj(
        "showUploadMultiple"              -> true,
        "showYesNoQuestionBeforeContinue" -> false,
        "showAddAnotherDocumentButton"    -> false,
        "showLanguageSelection"           -> true,
        "enableMultipleFilesPicker"       -> true
      )

      val deserialized = json.as[UploadDocumentsFeatures]
      deserialized shouldBe uploadDocumentsFeatures
    }
  }
}
