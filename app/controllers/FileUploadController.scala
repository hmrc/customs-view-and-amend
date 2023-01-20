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
import connector.UploadDocumentsConnector
import models.FileUploadJourney
import models.file_upload.UploadedFileMetadata
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errors.not_found

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction,
  modifySessionAction: ModifySessionAction,
  uploadDocumentsConnector: UploadDocumentsConnector,
  notFound: not_found,
  appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen verifyEmail andThen modifySessionAction

  final val chooseFiles: Action[AnyContent] =
    actions.async { case (request, session) =>
      implicit val r = request
      session.current.fileUploadJourney match {
        case None =>
          Future.successful(Redirect(routes.ClaimsOverviewController.show))

        case Some(FileUploadJourney(claim, documentTypeOpt, previouslyUploaded, nonce, submitted)) =>
          if (submitted)
            Future.successful(Redirect(routes.FileSubmissionController.showConfirmation))
          else {
            documentTypeOpt match {
              case None =>
                Future.successful(
                  Redirect(routes.FileSelectionController.onPageLoad(claim.caseNumber))
                )

              case Some(documentType) =>
                uploadDocumentsConnector
                  .startFileUpload(nonce, claim.caseNumber, claim.serviceType, documentType, previouslyUploaded)
                  .map { chooseFilesUrlOpt =>
                    Redirect(
                      s"${appConfig.fileUploadPublicUrl}${chooseFilesUrlOpt.getOrElse("/choose-files")}"
                    )
                  }
            }
          }
      }
    }

  final val receiveUpscanCallback: Action[UploadedFileMetadata] =
    (authenticate andThen modifySessionAction)
      .async(parse.json[UploadedFileMetadata]) { case (request, session) =>
        implicit val r                         = request
        val notification: UploadedFileMetadata = request.body
        session.current.fileUploadJourney match {
          case None =>
            Future.successful(Unauthorized)

          case Some(FileUploadJourney(claim, _, previouslyUploaded, nonce, submitted)) =>
            if (submitted)
              Future.successful(NoContent)
            else {
              if (notification.nonce == nonce) {
                session
                  .update(_.withUploadedFiles(notification.uploadedFiles))
                  .map {
                    case Some(_) => NoContent
                    case None    =>
                      // $COVERAGE-OFF$
                      InternalServerError
                    // $COVERAGE-ON$
                  }
              } else {
                Future.successful(Unauthorized)
              }
            }
        }
      }
}
