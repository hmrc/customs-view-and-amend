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
import models.{ClosedClaim, IdentifierRequest, InProgressClaim, PendingClaim}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import repositories.ClaimsCache
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{claim_detail, claims_closed, claims_in_progress, claims_overview, claims_pending}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimsOverview @Inject()(
                                mcc: MessagesControllerComponents,
                                authenticate: IdentifierAction,
                                verifyEmail: EmailAction,
                                financialsApiConnector: FinancialsApiConnector,
                                claimsOverview: claims_overview,
                                claimsClosed: claims_closed,
                                claimsPending: claims_pending,
                                claimsInProgress: claims_in_progress,
                                claimsCache: ClaimsCache,
                                claimDetail: claim_detail
                              )(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport {

  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  //TODO sepearate this file
  //TODO add case class with each claim type sequence
  //TODO write tests
  //TODO refactor views to be a bit more sensible
  //TODO add missing messages e.g. UNAUTHORIZED

  def show: Action[AnyContent] = actions.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { allClaims =>
      //TODO add number of notifications
      Ok(claimsOverview(0, allClaims))
    }
  }

  def showInProgressClaimList: Action[AnyContent] = actions.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      Ok(claimsInProgress(claims.inProgressClaims))
    }
  }

  def showPendingClaimList: Action[AnyContent] = actions.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      Ok(claimsPending(claims.pendingClaims))
    }
  }

  def showClosedClaimList: Action[AnyContent] = actions.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      Ok(claimsClosed(claims.closedClaims))
    }
  }

  def claimDetail(caseNumber: String): Action[AnyContent] = actions.async { implicit request =>
    claimsCache.hasCaseNumber(request.eori, caseNumber).flatMap { caseExists =>
      if (caseExists) {
        financialsApiConnector.getClaimInformation(caseNumber).map { result =>
          Ok(claimDetail(result))
        }
      } else {
        Future.successful(Ok("No Result Found"))
      }
    }
  }
}
