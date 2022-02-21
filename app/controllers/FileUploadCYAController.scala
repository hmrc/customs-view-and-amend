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

import actions.{EmailAction, IdentifierAction}
import cats.data.EitherT
import cats.data.EitherT.{fromOptionF, liftF}
import config.AppConfig
import connector.{FinancialsApiConnector, UploadDocumentsConnector}
import models.IdentifierRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents, Result}
import repositories.{ClaimsMongo, UploadedFilesCache}
import services.ClaimService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import viewmodels.FileUploadCheckYourAnswersHelper
import views.html.errors.not_found
import views.html.{upload_check_your_answers, upload_confirmation}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadCYAController @Inject()(
                                         mcc: MessagesControllerComponents,
                                         authenticate: IdentifierAction,
                                         verifyEmail: EmailAction,
                                         claimService: ClaimService,
                                         uploadedFilesCache: UploadedFilesCache,
                                         uploadDocumentsCheckYourAnswers: upload_check_your_answers,
                                         notFound: not_found,
                                       )(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport {


  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  //TODO: Handle different claim types NDRC / SCTY
  //TODO: Handle different documentType C285 / C&E1179

  def onPageLoad(caseNumber: String): Action[AnyContent] = actions.async { implicit request =>
    val result: EitherT[Future, Result, Result] = for {
      _ <- fromOptionF[Future, Result, ClaimsMongo](claimService.authorisedToView(caseNumber, request.eori), NotFound(notFound()))
      files <- liftF(uploadedFilesCache.retrieveCurrentlyUploadedFiles(caseNumber))
      helper = new FileUploadCheckYourAnswersHelper(files)
    } yield Ok(uploadDocumentsCheckYourAnswers(caseNumber, helper))
    result.merge
  }
}
