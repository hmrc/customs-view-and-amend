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
import controllers.actions.IdentifierAction
import models.{ClosedClaim, InProgressClaim, PendingClaim}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ClaimsCache
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{claim_detail, claims_closed, claims_in_progress, claims_overview, claims_pending}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimsOverview @Inject()(
                                mcc: MessagesControllerComponents,
                                authenticate: IdentifierAction,
                                financialsApiConnector: FinancialsApiConnector,
                                claimsOverview: claims_overview,
                                claimsClosed: claims_closed,
                                claimsPending: claims_pending,
                                claimsInProgress: claims_in_progress,
                                claimsCache: ClaimsCache,
                                claimDetail: claim_detail
                              )(implicit executionContext: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {
  def show: Action[AnyContent] = authenticate.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      val noOfInProgress = claims.collect { case e: InProgressClaim => e }.size
      val noOfPending = claims.collect { case e: PendingClaim => e }.size
      val noOfClosed = claims.collect { case e: ClosedClaim => e }.size
      //TODO add number of notifications
      Ok(claimsOverview(0, noOfPending, noOfClosed, noOfInProgress))
    }
  }

  def showInProgressClaimList: Action[AnyContent] = authenticate.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      val x = claims.collect { case e: InProgressClaim => e }
      Ok(claimsInProgress(x))
    }
  }

  def showPendingClaimList: Action[AnyContent] = authenticate.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      val x = claims.collect { case e: PendingClaim => e }
      Ok(claimsPending(x))
    }
  }

  def showClosedClaimList: Action[AnyContent] = authenticate.async { implicit request =>
    financialsApiConnector.getClaims(request.eori).map { claims =>
      val x = claims.collect { case e: ClosedClaim => e }
      Ok(claimsClosed(x))
    }
  }

  def claimDetail(caseNumber: String): Action[AnyContent] = authenticate.async { implicit request =>
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
