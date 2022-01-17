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

package uk.gov.hmrc.customsviewandamend.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.customsviewandamend.connector.FinancialsApiConnector
import uk.gov.hmrc.customsviewandamend.models.{ClosedClaim, InProgressClaim, PendingClaim}
import uk.gov.hmrc.customsviewandamend.views.html.claims_overview
import uk.gov.hmrc.customsviewandamend.views.html.claims_closed
import uk.gov.hmrc.customsviewandamend.views.html.claims_pending
import uk.gov.hmrc.customsviewandamend.views.html.claims_in_progress
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimsOverview @Inject()(
                                mcc: MessagesControllerComponents,
                                financialsApiConnector: FinancialsApiConnector,
                                claimsOverview: claims_overview,
                                claimsClosed: claims_closed,
                                claimsPending: claims_pending,
                                claimsInProgress: claims_in_progress
                              )(implicit executionContext: ExecutionContext)
  extends FrontendController(mcc) {

  def show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(claimsOverview()))
  }

  def showInProgressClaimList: Action[AnyContent] = Action.async { implicit request =>
    financialsApiConnector.getClaims().map { claims =>
      val x = claims.collect { case e: InProgressClaim => e }
      Ok(claimsInProgress(x))
    }
  }

  def showPendingClaimList: Action[AnyContent] = Action.async { implicit request =>
    financialsApiConnector.getClaims().map { claims =>
      val x = claims.collect { case e: PendingClaim => e }
      Ok(claimsPending(x))
    }
  }

  def showClosedClaimList: Action[AnyContent] = Action.async { implicit request =>
    financialsApiConnector.getClaims().map { claims =>
      val x = claims.collect { case e: ClosedClaim => e }
      Ok(claimsClosed(x))
    }
  }

  def claimDetail(caseNumber: String): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(s"Success $caseNumber"))

  }
}
