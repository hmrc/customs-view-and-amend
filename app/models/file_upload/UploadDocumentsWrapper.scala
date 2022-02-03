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
import models.ClaimType
import play.api.libs.json.{Json, OFormat}

case class UploadDocumentsWrapper(config: UploadDocumentsConfig)

object UploadDocumentsWrapper {

  def createPayload(nonce: Nonce,
                    caseNumber: String,
                    claimType: ClaimType,
                    searched: Boolean,
                    multipleUpload: Boolean
                   )(implicit appConfig: AppConfig): UploadDocumentsWrapper = {
    UploadDocumentsWrapper(
      config = UploadDocumentsConfig(
        nonce = nonce,
        initialNumberOfEmptyRows = Some(1),
        continueUrl = appConfig.backLinkUrl(controllers.routes.FileUploadController.continue(caseNumber).url),
        backlinkUrl = appConfig.backLinkUrl(controllers.routes.ClaimDetailController.claimDetail(caseNumber, claimType, searched).url),
        callbackUrl = appConfig.fileUploadCallBack,
        cargo = UploadCargo(caseNumber),
        content = Some(UploadDocumentsContent(
          serviceName = Some(appConfig.fileUploadServiceName),
          serviceUrl = Some(appConfig.homepage),
          accessibilityStatementUrl = Some(appConfig.fileUploadAccessibilityUrl),
          phaseBanner = Some(appConfig.fileUploadPhase),
          phaseBannerUrl = Some(appConfig.fileUploadPhaseUrl),
          userResearchBannerUrl = Some(appConfig.helpMakeGovUkBetterUrl),
          contactFrontendServiceId = Some(appConfig.contactFrontendServiceId)
        )),
        features = Some(UploadDocumentsFeatures(Some(multipleUpload)))
      )
    )
  }

  implicit val format: OFormat[UploadDocumentsWrapper] = Json.format[UploadDocumentsWrapper]
}