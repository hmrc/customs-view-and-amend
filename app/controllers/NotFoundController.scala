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

package controllers

import config.AppConfig
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.ErrorTemplate

import javax.inject.{Inject, Singleton}

@Singleton
class NotFoundController @Inject() (
  controllerComponents: MessagesControllerComponents,
  errorTemplate: ErrorTemplate
)(implicit val appConfig: AppConfig)
    extends FrontendController(controllerComponents)
    with I18nSupport {

  final val onPageLoad: Action[AnyContent] = Action { implicit request =>
    val messages = implicitly[Messages]
    NotFound(
      errorTemplate(
        messages("error-page-not-found.title"),
        messages("error-page-not-found.heading"),
        messages("error-page-not-found.link.message")
      )(request, messages, implicitly)
    )
  }
}
