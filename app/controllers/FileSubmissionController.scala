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

import actions.{EmailAction, IdentifierAction, ModifySessionAction}
import config.AppConfig
import connector.{FileSubmissionConnector, UploadDocumentsConnector}
import models.{EntryNumber, FileUploadJourney}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errors.not_found
import views.html.upload_confirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileSubmissionController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction,
  modifySessionAction: ModifySessionAction,
  fileSubmissionConnector: FileSubmissionConnector,
  uploadDocumentsConnector: UploadDocumentsConnector,
  notFound: not_found,
  upload_confirmation: upload_confirmation
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen verifyEmail andThen modifySessionAction

  final val submitFiles: Action[AnyContent] =
    actions.async { case (request, session) =>
      implicit val r = request

      session.current.fileUploadJourney match {
        case None =>
          Future.successful(Redirect(routes.ClaimsOverviewController.show))

        case Some(FileUploadJourney(claim, _, uploadedFiles, nonce, submitted)) =>
          if (submitted)
            Future.successful(
              Redirect(routes.FileSubmissionController.showConfirmation)
            )
          else {
            for {
              successfullyUploaded <- fileSubmissionConnector
                                        .submitFileToCDFPay(
                                          claim.declarationId,
                                          EntryNumber.isEntryNumber(claim.declarationId),
                                          request.eori,
                                          claim.serviceType,
                                          claim.caseNumber,
                                          uploadedFiles
                                        )
              _                    <- uploadDocumentsConnector.wipeData
              _                    <- session.update(_.withSubmitted)
            } yield
              if (successfullyUploaded)
                Redirect(routes.FileSubmissionController.showConfirmation)
              else
                throw new Exception("File upload submission has failed.")
          }
      }
    }

  final val showConfirmation: Action[AnyContent] =
    actions.async { case (request, session) =>
      implicit val r = request

      session.current.fileUploadJourney match {
        case None =>
          Future.successful(Redirect(routes.ClaimsOverviewController.show))

        case Some(FileUploadJourney(claim, _, _, _, submitted)) =>
          Future.successful(
            if (submitted)
              Ok(upload_confirmation(claim.caseNumber, request.verifiedEmail))
            else
              Redirect(routes.FileUploadController.chooseFiles.url)
          )
      }
    }
}
