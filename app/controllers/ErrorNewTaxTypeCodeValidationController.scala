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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errors.tax_type_code_validation

import javax.inject.{Inject, Singleton}

@Singleton
class ErrorNewTaxTypeCodeValidationController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  currentSession: CurrentSessionAction,
  allClaimsAction: AllClaimsAction,
  taxTypeCodeValidation: tax_type_code_validation
)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen currentSession andThen allClaimsAction

  final def showError(caseNumber: String): Action[AnyContent] =
    actions { case (request, allClaims) =>
      implicit val r = request
      allClaims.findByCaseNumber(caseNumber) match {
        case Some(claim) =>
          Ok(
            taxTypeCodeValidation(claim.caseNumber, routes.ClaimsOverviewController.show.url, appConfig.contactHmrcUrl)
          )
        case None        =>
          Redirect(routes.ClaimsOverviewController.show.url)
      }
    }
}
