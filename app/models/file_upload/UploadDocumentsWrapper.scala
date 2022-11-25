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

package models.file_upload

import config.AppConfig
import models.responses.ClaimType
import models.{FileSelection, ServiceType}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

case class UploadDocumentsWrapper(config: UploadDocumentsConfig, existingFiles: Seq[UploadedFile])

object UploadDocumentsWrapper {

  def createPayload(nonce: Nonce,
                    caseNumber: String,
                    serviceType: ServiceType,
                    claimType: ClaimType,
                    documentType: FileSelection,
                    previouslyUploaded: Seq[UploadedFile] = Seq.empty
                   )(implicit appConfig: AppConfig, messages: Messages): UploadDocumentsWrapper = {
    val continueUrl = controllers.routes.FileUploadCYAController.onPageLoad(caseNumber, serviceType)
    val backLinkUrl = controllers.routes.FileSelectionController.onPageLoad(caseNumber, serviceType, claimType, initialRequest = false).url
    val callBack = controllers.routes.FileUploadController.updateFiles()

    UploadDocumentsWrapper(
      config = UploadDocumentsConfig(
        nonce = nonce,
        initialNumberOfEmptyRows = Some(1),
        maximumNumberOfFiles = Some(10),
        maximumFileSizeBytes = Some(1024 * 1024 * 9),
        continueUrl = s"${appConfig.selfUrl}$continueUrl",
        backlinkUrl = s"${appConfig.selfUrl}$backLinkUrl",
        callbackUrl = s"${appConfig.fileUploadCallbackUrlPrefix}$callBack",
        continueAfterYesAnswerUrl = Some(s"${appConfig.selfUrl}$backLinkUrl"),
        cargo = UploadCargo(caseNumber),
        newFileDescription = documentType,
        allowedContentTypes = Some("image/jpeg,image/png,application/pdf"),
        allowedFileExtensions = Some(".jpg,.jpeg,.png,.pdf"),
        content = Some(UploadDocumentsContent(
          serviceName = Some(messages("service.name")),
          title = Some(s"Upload ${documentType.message.toLowerCase}"),
          descriptionHtml = Some(
            """<p class="govuk-body">""" +
              s"${documentType.message} files can be up to a maximum of 9MB size per file. " +
              "The selected file must be a JPG, PNG or PDF." +
              "</p>"
            ),
          serviceUrl = Some(appConfig.homepage),
          accessibilityStatementUrl = Some(appConfig.fileUploadAccessibilityUrl),
          phaseBanner = Some(appConfig.fileUploadPhase),
          phaseBannerUrl = Some(appConfig.fileUploadPhaseUrl),
          userResearchBannerUrl = Some(appConfig.helpMakeGovUkBetterUrl),
          contactFrontendServiceId = Some(appConfig.contactFrontendServiceId),
          yesNoQuestionText = Some("Add a different type of supporting evidence to your claim?"),
          yesNoQuestionRequiredError = Some("Select yes to add a different type of supporting document to your claim"),
          allowedFilesTypesHint = Some("The selected file must be a JPG, PNG or PDF"),
          fileUploadedProgressBarLabel = Some("Uploaded")
        )),
        features = Some(
          UploadDocumentsFeatures(
            showUploadMultiple = Some(appConfig.fileUploadMultiple),
            showYesNoQuestionBeforeContinue = Some(true)
          )
        )
      ),
      existingFiles = previouslyUploaded
    )
  }

  implicit val format: OFormat[UploadDocumentsWrapper] = Json.format[UploadDocumentsWrapper]
}