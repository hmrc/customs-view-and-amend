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

package config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  lazy val registerCdsUrl: String = config.get[String]("urls.cdsRegisterUrl")
  lazy val subscribeCdsUrl: String = config.get[String]("urls.cdsSubscribeUrl")
  lazy val loginUrl: String = config.get[String]("urls.login")
  lazy val loginContinueUrl: String = config.get[String]("urls.loginContinue")
  lazy val homepage: String = config.get[String]("urls.homepage")
  lazy val claimServiceUrl: String = config.get[String]("urls.claimService")
  lazy val signOutUrl: String = config.get[String]("urls.signOut")
  lazy val feedbackService = config.getOptional[String]("feedback.url").getOrElse("/feedback") +
    config.getOptional[String]("feedback.source").getOrElse("/CDS-FIN")
  lazy val contactFrontendServiceId: String = config.get[String]("contact-frontend.serviceId")

  lazy val helpMakeGovUkBetterUrl: String = config.get[String]("urls.helpMakeGovUkBetterUrl")

  private lazy val fileUploadHost: String = config.get[String]("file-upload.host")
  private lazy val fileUploadReturnHost: String = config.get[String]("file-upload.returnHost")
  lazy val fileUploadInitializeUrl: String = s"${config.get[String]("file-upload.initializeHost")}/upload-documents/initialize"
  lazy val fileUploadCallBack: String = config.get[String]("file-upload.uploadedFilesCallBackUrl")
  lazy val fileUploadServiceName: String = config.get[String]("file-upload.serviceName")
  lazy val fileUploadPhase: String = config.get[String]("file-upload.phaseBanner")
  lazy val fileUploadPhaseUrl: String = config.get[String]("file-upload.phaseBannerUrl")
  lazy val fileUploadAccessibilityUrl: String = config.get[String]("file-upload.accessibilityStatement")

  def absoluteLink(relativeLocation: String) = s"$fileUploadReturnHost$relativeLocation"
  def fileUploadLink(relativeLocation: String) = s"$fileUploadHost$relativeLocation"

  lazy val timeout: Int = config.get[Int]("timeout.timeout")
  lazy val countdown: Int = config.get[Int]("timeout.countdown")

  lazy val itemsPerPage: Int = config.get[Int]("pagination.itemsPerPage")


  lazy val customsDataStore: String = servicesConfig.baseUrl("customs-data-store") +
    config.get[String]("microservice.services.customs-data-store.context")

  lazy val emailFrontendUrl: String = config.get[String]("urls.emailFrontend")

  lazy val customsFinancialsApi: String = servicesConfig.baseUrl("customs-financials-api") +
    config.getOptional[String]("customs-financials-api.context").getOrElse("/customs-financials-api")

}
