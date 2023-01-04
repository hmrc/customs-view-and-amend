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

package config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {
  lazy val registerCdsUrl: String           = config.get[String]("urls.cdsRegisterUrl")
  lazy val subscribeCdsUrl: String          = config.get[String]("urls.cdsSubscribeUrl")
  lazy val loginUrl: String                 = config.get[String]("urls.login")
  lazy val loginContinueUrl: String         = config.get[String]("urls.loginContinue")
  lazy val homepage: String                 = config.get[String]("urls.homepage")
  lazy val claimServiceUrl: String          = config.get[String]("urls.claimService")
  lazy val guidancePage: String             = config.get[String]("urls.guidancePage")
  lazy val signOutUrl: String               = config.get[String]("urls.signOut")
  lazy val feedbackService                  = config.getOptional[String]("feedback.url").getOrElse("/feedback") +
    config.getOptional[String]("feedback.source").getOrElse("/CDSRC")
  lazy val contactFrontendServiceId: String = config.get[String]("contact-frontend.serviceId")

  lazy val helpMakeGovUkBetterUrl: String = config.get[String]("urls.helpMakeGovUkBetterUrl")

  lazy val selfUrl: String                     = servicesConfig.getString("self.url")
  lazy val fileUploadBaseUrl: String           =
    servicesConfig.baseUrl("upload-documents-frontend")
  lazy val fileUploadCallbackUrlPrefix: String =
    servicesConfig.getConfString("upload-documents-frontend.callback-url-prefix", "")
  lazy val fileUploadPublicUrl: String         =
    servicesConfig.getConfString("upload-documents-frontend.public-url", "")
  lazy val fileUploadInitializationUrl: String = s"$fileUploadBaseUrl/internal/initialize"
  lazy val fileUploadWipeOutUrl: String        = s"$fileUploadBaseUrl/internal/wipe-out"
  lazy val fileUploadServiceName: String       =
    config.get[String]("microservice.services.upload-documents-frontend.serviceName")
  lazy val fileUploadPhase: String             = config.get[String]("microservice.services.upload-documents-frontend.phaseBanner")
  lazy val fileUploadPhaseUrl: String          =
    config.get[String]("microservice.services.upload-documents-frontend.phaseBannerUrl")
  lazy val fileUploadAccessibilityUrl: String  =
    config.get[String]("microservice.services.upload-documents-frontend.accessibilityStatement")
  lazy val fileUploadMultiple: Boolean         =
    config.get[Boolean]("microservice.services.upload-documents-frontend.multiple-upload")
  lazy val fileUploadSummaryUrl: String        = s"$fileUploadPublicUrl/upload-customs-documents/summary"

  lazy val timeout: Int      = config.get[Int]("timeout.timeout")
  lazy val countdown: Int    = config.get[Int]("timeout.countdown")
  lazy val itemsPerPage: Int = config.get[Int]("pagination.itemsPerPage")

  lazy val customsDataStore: String = servicesConfig.baseUrl("customs-data-store") +
    config.get[String]("microservice.services.customs-data-store.context")

  lazy val emailFrontendUrl: String = config.get[String]("urls.emailFrontend")

  lazy val cdsReimbursementClaim: String = servicesConfig.baseUrl("cds-reimbursement-claim") +
    config
      .getOptional[String]("cds-reimbursement-claim.context")
      .getOrElse("/cds-reimbursement-claim")

}
