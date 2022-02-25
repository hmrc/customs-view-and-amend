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

package viewmodels

import config.AppConfig
import models.file_upload.UploadedFile
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}

class FileUploadCheckYourAnswersHelper(files: Seq[UploadedFile])(implicit messages: Messages, appConfig: AppConfig) extends SummaryListRowHelper {

  def rows: Seq[SummaryListRow] = {
    files.groupBy(_.description).map { case (description, uploadedFiles) =>
      summaryListRow(
        description.message,
        uploadedFiles.map(v => s"<p>${v.fileName}</p>").mkString("\n"),
        actions = Actions(
          items = Seq(
            ActionItem(
              href = appConfig.fileUploadSummaryUrl,
              content = span(messages("site.change")),
              visuallyHiddenText = Some(messages("file.uploaded.visually.hidden"))
            )
          )
        ),
        secondValue = None
      )
    }.toSeq
  }
}
