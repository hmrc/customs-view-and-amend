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

import config.AppConfig
import connector.FinancialsApiConnector
import controllers.actions.{EmailAction, IdentifierAction}
import models.IdentifierRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import viewmodels.{ClosedClaimListViewModel, InProgressClaimListViewModel, PendingClaimListViewModel}
import views.html.{claims_closed, claims_in_progress, claims_pending}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ClaimListController @Inject()(mcc: MessagesControllerComponents,
                                    authenticate: IdentifierAction,
                                    verifyEmail: EmailAction,
                                    financialsApiConnector: FinancialsApiConnector,
                                    claimsClosed: claims_closed,
                                    claimsPending: claims_pending,
                                    claimsInProgress: claims_in_progress,
                                   )(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport {

  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  def showInProgressClaimList(page: Option[Int]): Action[AnyContent] = actions.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      Ok(claimsInProgress(InProgressClaimListViewModel(claims.inProgressClaims, page)))
    }
  }

  def showPendingClaimList(page: Option[Int]): Action[AnyContent] = actions.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      Ok(claimsPending(PendingClaimListViewModel(claims.pendingClaims, page)))
    }
  }

  def showClosedClaimList(page: Option[Int]): Action[AnyContent] = actions.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      Ok(claimsClosed(ClosedClaimListViewModel(claims.closedClaims, page)))
    }
  }
}
