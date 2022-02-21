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
import cats.data.EitherT._
import config.AppConfig
import connector.{FinancialsApiConnector, UploadDocumentsConnector}
import forms.{C285FormProvider, CE1179FormProvider}
import models.{C285FileSelection, CE1179FileSelection, ClaimType, IdentifierRequest}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import repositories.{ClaimsCache, ClaimsMongo}
import services.ClaimService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errors.not_found
import views.html.file_selection

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileSelectionController @Inject()(uploadDocumentsConnector: UploadDocumentsConnector,
                                        claimService: ClaimService,
                                        mcc: MessagesControllerComponents,
                                        fileSelection: file_selection,
                                        c285FormProvider: C285FormProvider,
                                        ce1179FormProvider: CE1179FormProvider,
                                        notFound: not_found,
                                        authenticate: IdentifierAction,
                                        verifyEmail: EmailAction)(implicit executionContext: ExecutionContext, appConfig: AppConfig) extends FrontendController(mcc) with I18nSupport {

  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail
  val c285formProvider: Form[C285FileSelection] = c285FormProvider()
  val ce1179formProvider: Form[CE1179FileSelection] = ce1179FormProvider()

  //TODO process ce1179 / c285

  def onPageLoad(caseNumber: String, claimType: ClaimType, searched: Boolean, initialRequest: Boolean): Action[AnyContent] = actions.async { implicit request =>
    val result: EitherT[Future, Result, Result] = for {
      _ <- fromOptionF(claimService.authorisedToView(caseNumber, request.eori), NotFound(notFound()))
      _ <- liftF(claimService.clearUploaded(caseNumber, initialRequest))
    } yield Ok(fileSelection(c285formProvider, caseNumber, claimType, searched, C285FileSelection.options(c285formProvider)))
    result.merge
  }

  def onSubmit(caseNumber: String, claimType: ClaimType, searched: Boolean): Action[AnyContent] = actions.async { implicit request =>
    c285formProvider.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(fileSelection(formWithErrors, caseNumber, claimType, searched, C285FileSelection.options(c285formProvider)))),
      documentType =>
        (for {
          _ <- fromOptionF(claimService.authorisedToView(caseNumber, request.eori), NotFound(notFound()))
          result <- fromOptionF(uploadDocumentsConnector.startFileUpload(caseNumber, claimType, searched, documentType)
            .map(_.map(relativeUrl => Redirect(s"${appConfig.fileUploadPublicUrl}$relativeUrl"))), NotFound(notFound()))
        } yield result).merge
    )
  }
}
