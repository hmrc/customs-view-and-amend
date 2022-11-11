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

import actions.IdentifierAction
import cats.syntax.EqOps
import cats.data.EitherT._
import cats.implicits.catsSyntaxEq
import config.AppConfig
import connector.{DataStoreConnector, FinancialsApiConnector}
import models.{ClaimDetail, ClaimStatus, Closed, InProgress, Pending, ServiceType}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.{ClaimsCache, ClaimsMongo}
import services.ClaimService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.claim_detail
import views.html.errors.not_found

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimDetailController @Inject()(mcc: MessagesControllerComponents,
                                      authenticate: IdentifierAction,
                                      dataStoreConnector: DataStoreConnector,
                                      financialsApiConnector: FinancialsApiConnector,
                                      claimService: ClaimService,
                                      claimDetail: claim_detail,
                                      claimsCache: ClaimsCache,
                                      notFound: not_found)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport {

  def inProgressClaimDetail(caseNumber: String, serviceType: ServiceType, searched: Boolean): Action[AnyContent] =
    claimDetail(caseNumber, serviceType, searched, InProgress)

  def closedClaimDetail(caseNumber: String, serviceType: ServiceType, searched: Boolean): Action[AnyContent] =
    claimDetail(caseNumber, serviceType, searched, Closed)

  def pendingClaimDetail(caseNumber: String, serviceType: ServiceType, searched: Boolean): Action[AnyContent] =
    claimDetail(caseNumber, serviceType, searched, Pending)

  private def claimDetail(caseNumber: String, serviceType: ServiceType, searched: Boolean, expectedStatus: ClaimStatus): Action[AnyContent] = authenticate.async { implicit request =>
    (for {
      claims <- fromOptionF(claimService.authorisedToView(caseNumber, request.eori), NotFound(notFound()))
      lrn = claims.claims.find(_.caseNumber == caseNumber).flatMap(_.lrn)
      email <- fromOptionF(dataStoreConnector.getEmail(request.eori).map(_.toOption), NotFound(notFound()))
      claim <- fromOptionF[Future, Result, ClaimDetail](financialsApiConnector.getClaimInformation(caseNumber, serviceType, lrn), NotFound(notFound()))
    } yield {
      if (claim.claimStatus == expectedStatus)
        Ok(claimDetail(claim, searched, email.value))
      else
        NotFound(notFound())
    }).merge
  }
}
