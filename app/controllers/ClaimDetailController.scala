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
import repositories.ClaimsCache
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.claim_detail

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimDetailController @Inject()(mcc: MessagesControllerComponents,
                                      authenticate: IdentifierAction,
                                      financialsApiConnector: FinancialsApiConnector,
                                      verifyEmail: EmailAction,
                                      claimsCache: ClaimsCache,
                                      claimDetail: claim_detail)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport {

  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  def claimDetail(caseNumber: String): Action[AnyContent] = actions.async { implicit request =>
    claimsCache.hasCaseNumber(request.eori, caseNumber).flatMap { caseExists =>
      if (caseExists) {
        financialsApiConnector.getClaimInformation(caseNumber).map { result =>
          Ok(claimDetail(result))
        }
      } else {
        Future.successful(NotFound)
      }
    }
  }

}
