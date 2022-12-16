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
import connector.{ClaimsConnector, DataStoreConnector}
import models.responses.C285
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.claim_detail
import views.html.errors.not_found

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimDetailController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction,
  allClaimsAction: AllClaimsAction,
  dataStoreConnector: DataStoreConnector,
  claimsConnector: ClaimsConnector,
  claimDetail: claim_detail,
  notFound: not_found
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  val preconditions = authenticate andThen verifyEmail andThen allClaimsAction

  def claimDetail(caseNumber: String): Action[AnyContent] =
    preconditions.async { case (request, allClaims) =>
      implicit val r = request
      allClaims.findByCaseNumber(caseNumber) match {
        case Some(claim) =>
          claimsConnector
            .getClaimInformation(caseNumber, claim.serviceType, claim.lrn)
            .map {
              case None =>
                NotFound(notFound())
                  .withHeaders("X-Explanation" -> "CLAIM_INFORMATION_NOT_FOUND")

              case Some(claimDetails) =>
                val fileSelectionUrl =
                  routes.FileSelectionController
                    .onPageLoad(
                      claimDetails.caseNumber,
                      claimDetails.serviceType,
                      claimDetails.claimType.getOrElse(C285),
                      initialRequest = false
                    )
                Ok(claimDetail(claimDetails, request.verifiedEmail, fileSelectionUrl.url))
            }

        case _ =>
          Future.successful(
            NotFound(notFound())
              .withHeaders("X-Explanation" -> "NOT_AUTHORISED_TO_VIEW")
          )
      }
    }
}
