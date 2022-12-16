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
import models.{AllClaims, Claim, IdentifierRequest}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.SearchCache
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.search_claims

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimSearch @Inject() (
  connector: ClaimsConnector,
  mcc: MessagesControllerComponents,
  searchCache: SearchCache,
  searchClaim: search_claims,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  val formProvider: Form[String]                            = SearchFormHelper.form
  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  def onPageLoad(): Action[AnyContent] = actions.async { implicit request =>
    Future.successful(Ok(searchClaim()))
  }

  def onSubmit(): Action[AnyContent] = actions.async { implicit request =>
    formProvider
      .bindFromRequest()
      .fold(
        _ => Future.successful(BadRequest(searchClaim())),
        query => getClaim(query).map(claim => Ok(searchClaim(claim, Some(query), searched = true)))
      )
  }

  def getClaim(query: String)(implicit request: IdentifierRequest[AnyContent]): Future[Seq[Claim]] =
    searchCache
      .get(request.eori)
      .map[Future[AllClaims]] {
        case Some(searchQuery) => Future.successful(searchQuery.claims)
        case _                 =>
          connector.getAllClaims.map { claims =>
            searchCache.set(request.eori, claims, query)
            claims
          }
      }
      .flatten
      .map(claims => claims.findClaim(query))
}
