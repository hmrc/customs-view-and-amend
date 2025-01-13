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

import actions.{CurrentSessionAction, IdentifierAction, ModifySessionAction}
import config.AppConfig
import connector.UploadDocumentsConnector
import forms.FileSelectionForm
import forms.FormUtils.*
import models.{FileSelection, FileUploadJourney, SessionData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errors.not_found
import views.html.file_selection

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileSelectionController @Inject() (
  uploadDocumentsConnector: UploadDocumentsConnector,
  mcc: MessagesControllerComponents,
  fileSelection: file_selection,
  notFound: not_found,
  authenticate: IdentifierAction,
  currentSession: CurrentSessionAction,
  modifySessionAction: ModifySessionAction
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  private val actions = authenticate andThen currentSession andThen modifySessionAction

  final def onPageLoad(caseNumber: String): Action[AnyContent] =
    actions.async { case (request, session) =>
      implicit val r = request
      session
        .update(_.withInitialFileUploadData(caseNumber))
        .map {
          case Some(SessionData(_, _, _, Some(allClaims), Some(fileUploadJourney))) =>
            val form: Form[FileSelection] = FileSelectionForm.form
            Ok(
              fileSelection(
                form.withDefault(fileUploadJourney.documentType),
                fileUploadJourney.claim.serviceType,
                fileUploadJourney.claim.caseNumber,
                FileSelection.options(form)
              )
            )

          case _ =>
            Redirect(routes.ClaimDetailController.claimDetail(caseNumber))
        }
    }

  final val onSubmit: Action[AnyContent] =
    actions.async { case (request, session) =>
      implicit val r = request
      session.current.fileUploadJourney match {
        case None =>
          Future.successful(Redirect(routes.ClaimsOverviewController.show))

        case Some(FileUploadJourney(claim, _, previouslyUploaded, nonce, submitted)) =>
          if (submitted)
            Future.successful(Redirect(routes.FileSubmissionController.showConfirmation))
          else {
            val form: Form[FileSelection] = FileSelectionForm.form
            form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      fileSelection(formWithErrors, claim.serviceType, claim.caseNumber, FileSelection.options(form))
                    )
                  ),
                documentType =>
                  session.update(_.withDocumentType(documentType)).map { _ =>
                    Redirect(routes.FileUploadController.chooseFiles)
                  }
              )
          }
      }
    }

}
