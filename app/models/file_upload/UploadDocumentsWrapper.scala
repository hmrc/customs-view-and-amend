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

package models.file_upload

import config.AppConfig
import models.{FileSelection, Nonce, ServiceType}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
case class UploadDocumentsWrapper(config: UploadDocumentsConfig, existingFiles: Seq[UploadedFile])

object UploadDocumentsWrapper {

  def createPayload(
    nonce: Nonce,
    caseNumber: String,
    serviceType: ServiceType,
    documentType: FileSelection,
    previouslyUploaded: Seq[UploadedFile] = Seq.empty
  )(implicit appConfig: AppConfig, messages: Messages): UploadDocumentsWrapper = {
    val claimsDatailsUrl  = controllers.routes.ClaimDetailController.claimDetail(caseNumber)
    val continueUrl       = controllers.routes.FileSubmissionController.submitFiles
    val chooseFileTypeUrl = controllers.routes.FileSelectionController.onPageLoad(caseNumber).url
    val callBackUrl       = controllers.routes.FileUploadController.receiveUpscanCallback

    UploadDocumentsWrapper(
      config = UploadDocumentsConfig(
        nonce = nonce,
        initialNumberOfEmptyRows = Some(1),
        maximumNumberOfFiles = Some(100),
        maximumFileSizeBytes = Some(1024 * 1024 * 9),
        continueUrl = s"${appConfig.selfUrl}$continueUrl",
        callbackUrl = s"${appConfig.fileUploadCallbackUrlPrefix}$callBackUrl",
        continueAfterYesAnswerUrl = Some(s"${appConfig.selfUrl}$chooseFileTypeUrl"),
        sendoffUrl = Some(s"${appConfig.selfUrl}$claimsDatailsUrl"),
        cargo = UploadCargo(caseNumber),
        newFileDescription = documentType,
        allowedContentTypes = Some(
          "application/pdf,image/jpeg,image/png,text/csv,text/plain,application/vnd.ms-outlook,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.oasis.opendocument.text,application/vnd.oasis.opendocument.spreadsheet"
        ),
        allowedFileExtensions =
          Some(".pdf,.png,.jpg,.jpeg,.csv,.txt,.msg,.pst,.ost,.eml,.doc,.docx,.xls,.xlsx,.ods,.odt"),
        content = Some(
          UploadDocumentsContent(
            serviceName = Some(messages("service.name")),
            title = Some(messages("file-upload.title", documentType.message.toLowerCase)),
            descriptionHtml = Some(messages("file-upload.description", documentType.message)),
            serviceUrl = Some(appConfig.guidancePage),
            accessibilityStatementUrl = Some(appConfig.accessibilityStatementUrl),
            phaseBanner = Some(appConfig.fileUploadPhase),
            phaseBannerUrl = Some(appConfig.betaFeedbackUrl),
            contactFrontendServiceId = Some(appConfig.contactFrontendServiceId),
            yesNoQuestionText = Some(messages("file-upload.yes-no-question-text.h2")),
            yesNoQuestionRequiredError = Some(messages("file-upload.yes-no-question.error.required")),
            allowedFilesTypesHint = Some(messages("file-upload.allowed-file-types-hint")),
            fileUploadedProgressBarLabel = Some(messages("file-upload.progress-bar-label")),
            chooseFirstFileLabel = Some(messages("file-upload.upload-label", documentType.message.toLowerCase)),
            chooseNextFileLabel = Some(messages("file-upload.another-upload-label", documentType.message.toLowerCase)),
            signOutUrl = Some(appConfig.signedOutPageUrl),
            timedOutUrl = Some(appConfig.signedOutPageUrl),
            timeoutSeconds = Some(appConfig.timeout),
            countdownSeconds = Some(appConfig.countdown)
          )
        ),
        features = Some(
          UploadDocumentsFeatures(
            showUploadMultiple = Some(appConfig.fileUploadMultiple),
            showLanguageSelection = Some(false),
            showYesNoQuestionBeforeContinue = Some(true),
            enableMultipleFilesPicker = Some(true)
          )
        )
      ),
      existingFiles = previouslyUploaded
    )
  }

  implicit val format: OFormat[UploadDocumentsWrapper] =
    Json.format[UploadDocumentsWrapper]

}
