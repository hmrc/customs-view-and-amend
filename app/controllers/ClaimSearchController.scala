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

import actions.{AllClaimsAction, CurrentSessionAction, IdentifierAction}
import config.AppConfig
import connector.ClaimsConnector
import forms.SearchFormHelper
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.search_claims

import javax.inject.{Inject, Singleton}

@Singleton
class ClaimSearchController @Inject() (
  connector: ClaimsConnector,
  mcc: MessagesControllerComponents,
  searchClaim: search_claims,
  authenticate: IdentifierAction,
  currentSession: CurrentSessionAction,
  allClaimsAction: AllClaimsAction
)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen currentSession andThen allClaimsAction

  final val onPageLoad: Action[AnyContent] =
    actions { case (request, _) =>
      implicit val r = request
      Ok(searchClaim(form = SearchFormHelper.form))
    }

  final val onSubmit: Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r = request
      SearchFormHelper.form
        .bindFromRequest()
        .fold(
          errors => BadRequest(searchClaim(form = errors)),
          query => {
            val claims = allClaims.searchForClaim(query)
            Ok(searchClaim(claims, Some(query), form = SearchFormHelper.form))
          }
        )
    }
}
