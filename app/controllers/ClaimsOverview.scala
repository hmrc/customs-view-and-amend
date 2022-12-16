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

import actions.{EmailAction, IdentifierAction}
import config.AppConfig
import connector.ClaimsConnector
import forms.SearchFormHelper
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SearchCache
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.claims_overview

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ClaimsOverview @Inject() (
  mcc: MessagesControllerComponents,
  searchCache: SearchCache,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction,
  claimsConnector: ClaimsConnector,
  claimsOverview: claims_overview
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  def show: Action[AnyContent] = (authenticate andThen verifyEmail).async { implicit request =>
    searchCache
      .removeSearch(request.eori)
      .flatMap { _ =>
        claimsConnector.getAllClaims
          .map(allClaims =>
            Ok(
              claimsOverview(
                0,
                allClaims,
                SearchFormHelper.form,
                routes.ClaimSearch.onSubmit(),
                request.companyName.orNull,
                request.eori
              )
            )
          )
      }
  }

}
