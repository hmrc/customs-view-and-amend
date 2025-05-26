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

import actions.{AllClaimsAction, CurrentSessionAction, IdentifierAction, XiEoriAction}
import config.AppConfig
import forms.SearchFormHelper
import models.RequestWithSessionData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{claims_overview, search_claims}

import javax.inject.{Inject, Singleton}
import models.NDRC
import models.SCTY

@Singleton
class ClaimsOverviewController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  currentSession: CurrentSessionAction,
  allClaimsAction: AllClaimsAction,
  xiEoriAction: XiEoriAction,
  claimsOverview: claims_overview,
  searchClaim: search_claims
)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen currentSession andThen xiEoriAction andThen allClaimsAction

  final val show: Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r: RequestWithSessionData[_] = request
      Ok(
        claimsOverview(
          0,
          allClaims,
          SearchFormHelper.form,
          routes.ClaimsOverviewController.onSubmit,
          request.companyName.orNull,
          request.eori
        )
      )
    }

  final val onSubmit: Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r = request
      SearchFormHelper.form
        .bindFromRequest()
        .fold(
          errors =>
            BadRequest(
              claimsOverview(
                0,
                allClaims,
                errors,
                routes.ClaimsOverviewController.onSubmit,
                request.companyName.orNull,
                request.eori
              )
            ),
          query => {
            val q      = query.toUpperCase.trim()
            val claims = allClaims.searchForClaim(q)
            claims.headOption match {
              case Some(firstClaim) if claims.size == 1 =>
                Redirect(routes.ClaimDetailController.claimDetail(firstClaim.caseNumber))

              case None
                  if claims.isEmpty
                    && (NDRC.caseNumberRegex.matches(q)
                      || SCTY.caseNumberRegex.matches(q)) =>
                Redirect(routes.ClaimDetailController.claimDetail(q))

              case _ =>
                Ok(searchClaim(claims, Some(query), form = SearchFormHelper.form))
            }
          }
        )
    }
}
