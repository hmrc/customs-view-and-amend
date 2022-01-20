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

import connector.FinancialsApiConnector
import controllers.actions.{EmailAction, IdentifierAction}
import forms.SearchFormProvider
import models.IdentifierRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.search_claims

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimSearch @Inject()(connector: FinancialsApiConnector,
                            mcc: MessagesControllerComponents,
                            searchForm: SearchFormProvider,
                            searchClaim: search_claims,
                            authenticate: IdentifierAction,
                            verifyEmail: EmailAction)(implicit executionContext: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  val formProvider: Form[String] = searchForm()
  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  def onPageLoad(): Action[AnyContent] = actions.async { implicit request =>
      Future.successful(Ok(searchClaim(formProvider)))
  }

  def search(): Action[AnyContent] = actions.async { implicit request =>
    formProvider.bindFromRequest().fold(
      _ => Future.successful(Ok(searchClaim(formProvider))),
      query =>
        connector.getClaims(request.eori).map { allClaims =>
          Ok(searchClaim(formProvider, allClaims.findClaim(query), Some(query)))
        }
    )
  }
}
