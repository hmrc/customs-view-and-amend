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
import cats.data.EitherT.fromOptionF
import config.AppConfig
import connector.{FinancialsApiConnector, UploadDocumentsConnector}
import models.{ClaimType, IdentifierRequest}
import models.file_upload.UploadedFileMetadata
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents, Result}
import repositories.{ClaimsCache, ClaimsMongo, UploadedFilesCache}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errors.not_found
import views.html.upload_confirmation

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadController @Inject()(
                                      mcc: MessagesControllerComponents,
                                      authenticate: IdentifierAction,
                                      verifyEmail: EmailAction,
                                      uploadDocumentsConnector: UploadDocumentsConnector,
                                      financialsApiConnector: FinancialsApiConnector,
                                      claimsCache: ClaimsCache,
                                      uploadedFilesCache: UploadedFilesCache,
                                      notFound: not_found,
                                      confirmation: upload_confirmation
                                    )(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport {

  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  def start(caseNumber: String, claimType: ClaimType, searched: Boolean): Action[AnyContent] = actions.async { implicit request =>
    (for {
      _ <- EitherT.liftF(financialsApiConnector.getClaims(request.eori))
      _ <- fromOptionF[Future, Result, ClaimsMongo](claimsCache.getSpecificCase(request.eori, caseNumber), NotFound(notFound()))
      result <- fromOptionF(uploadDocumentsConnector.initializeNewFileUpload(caseNumber, claimType, searched).map(_.map(relativeUrl => Redirect(appConfig.fileUploadUrl(relativeUrl)))), NotFound(notFound()))
    } yield result).merge
  }

  def updateFiles(): Action[UploadedFileMetadata] = Action.async(parse.json[UploadedFileMetadata]) { implicit request =>
    request.body.cargo match {
      case Some(value) => uploadedFilesCache.updateRecord(value.caseNumber, request.body).map { _ => NoContent }
      case None => Future.successful(BadRequest)
    }
  }

  def continue(caseNumber: String): Action[AnyContent] = actions.async { implicit request =>
    //TODO integrate with DEC64 for file upload
    Future.successful(Ok(confirmation(caseNumber)))
  }
}
