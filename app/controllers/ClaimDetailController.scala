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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.claim_detail
import views.html.errors.not_found

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import models.ServiceType
import models.NDRC
import models.SCTY
import models.AuthorisedRequestWithSessionData

@Singleton
class ClaimDetailController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  currentSession: CurrentSessionAction,
  allClaimsAction: AllClaimsAction,
  claimsConnector: ClaimsConnector,
  claimDetail: claim_detail,
  notFound: not_found
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen currentSession andThen allClaimsAction

  final def claimDetail(caseNumber: String): Action[AnyContent] =
    actions.async { case (request, allClaims) =>
      implicit val r = request
      allClaims.findByCaseNumber(caseNumber) match {
        case Some(claim) =>
          showClaimDetail(caseNumber, claim.serviceType, claim.lrn, checkClaimPermission = false)

        case _ =>
          caseNumber match {
            case NDRC.caseNumberRegex() =>
              showClaimDetail(caseNumber, NDRC, None, checkClaimPermission = true)

            case SCTY.caseNumberRegex() =>
              showClaimDetail(caseNumber, SCTY, None, checkClaimPermission = true)

            case _ =>
              Future.successful(
                NotFound(notFound())
                  .withHeaders("X-Explanation" -> "CLOSED_CASE_NUMBER_INVALID")
              )
          }

      }
    }

  def showClaimDetail(caseNumber: String, serviceType: ServiceType, lrn: Option[String], checkClaimPermission: Boolean)(
    using request: AuthorisedRequestWithSessionData[AnyContent]
  ) =
    claimsConnector
      .getClaimInformation(caseNumber, serviceType, lrn)
      .map {
        case Right(Some(claimDetails)) =>
          if !checkClaimPermission || claimDetails.isConnectedTo(request.eori)
          then
            val fileSelectionUrl = routes.FileSelectionController.onPageLoad(claimDetails.caseNumber)
            Ok(claimDetail(claimDetails, request.verifiedEmail, fileSelectionUrl.url))
          else
            NotFound(notFound())
              .withHeaders("X-Explanation" -> "CLAIM_INFORMATION_ACCESS_FORBIDDEN")

        case Right(None) =>
          NotFound(notFound())
            .withHeaders("X-Explanation" -> "CLAIM_INFORMATION_NOT_FOUND")

        case Left("ERROR_HTTP_500") =>
          Redirect(routes.ErrorNewTaxTypeCodeValidationController.showError(caseNumber))

        case Left(other) =>
          NotFound(notFound())
            .withHeaders("X-Explanation" -> other)
      }
}
