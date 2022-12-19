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

package controllers

import actions.{AllClaimsAction, EmailAction, IdentifierAction}
import config.AppConfig
import forms.SearchFormHelper
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.claims_overview

import javax.inject.{Inject, Singleton}

@Singleton
class ClaimsOverviewController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction,
  allClaimsAction: AllClaimsAction,
  claimsOverview: claims_overview
)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen verifyEmail andThen allClaimsAction

  final val show: Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r = request
      Ok(
        claimsOverview(
          0,
          allClaims,
          SearchFormHelper.form,
          routes.ClaimSearchController.onSubmit,
          request.companyName.orNull,
          request.eori
        )
      )
    }

}
