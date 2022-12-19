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
import connector.ClaimsConnector
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import viewmodels.{ClosedClaimListViewModel, InProgressClaimListViewModel, PendingClaimListViewModel}
import views.components.hints.DropdownHints
import views.html.{claims_closed, claims_in_progress, claims_pending}

import javax.inject.{Inject, Singleton}

@Singleton
class ClaimListController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction,
  allClaimsAction: AllClaimsAction,
  claimsConnector: ClaimsConnector,
  claimsClosed: claims_closed,
  claimsPending: claims_pending,
  claimsInProgress: claims_in_progress
)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen verifyEmail andThen allClaimsAction

  private val caseStatusHints: DropdownHints =
    DropdownHints.range(elementIndex = 0, maxHints = 6)

  final def showInProgressClaimList(page: Option[Int]): Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r = request
      Ok(claimsInProgress(InProgressClaimListViewModel(allClaims.inProgressClaims, page)))
    }

  final def showPendingClaimList(page: Option[Int]): Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r = request
      Ok(claimsPending(PendingClaimListViewModel(allClaims.pendingClaims, page)))
    }

  final def showClosedClaimList(page: Option[Int]): Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r = request
      Ok(claimsClosed(ClosedClaimListViewModel(allClaims.closedClaims, page), caseStatusHints))
    }
}
