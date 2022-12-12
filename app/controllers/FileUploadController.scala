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
import connector.{DataStoreConnector, FileSubmissionConnector, ClaimsConnector, UploadDocumentsConnector}
import models.file_upload.UploadedFileMetadata
import models.{ClaimDetail, IdentifierRequest, ServiceType}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents, Result}
import repositories.{ClaimsMongo, UploadedFilesCache}
import services.ClaimService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errors.not_found
import views.html.upload_confirmation

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadController @Inject() (
  mcc: MessagesControllerComponents,
  authenticate: IdentifierAction,
  verifyEmail: EmailAction,
  claimService: ClaimService,
  claimsConnector: ClaimsConnector,
  dataStoreConnector: DataStoreConnector,
  uploadedFilesCache: UploadedFilesCache,
  uploadDocumentsConnector: UploadDocumentsConnector,
  confirmation: upload_confirmation,
  notFound: not_found,
  fileSubmissionConnector: FileSubmissionConnector
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  val actions: ActionBuilder[IdentifierRequest, AnyContent] = authenticate andThen verifyEmail

  def updateFiles(): Action[UploadedFileMetadata] = Action.async(parse.json[UploadedFileMetadata]) { implicit request =>
    request.body.cargo match {
      case Some(value) => uploadedFilesCache.updateRecord(value.caseNumber, request.body).map(_ => NoContent)
      case None        => Future.successful(BadRequest)
    }
  }

  def continue(caseNumber: String, serviceType: ServiceType): Action[AnyContent] = actions.async { implicit request =>
    val result: EitherT[Future, Result, Result] = for {
      _                    <- fromOptionF[Future, Result, ClaimsMongo](
                                claimService.authorisedToView(caseNumber, request.eori),
                                NotFound(notFound()).withHeaders("X-Explanation" -> "NOT_AUTHORISED_TO_VIEW")
                              )
      claim                <- fromOptionF[Future, Result, ClaimDetail](
                                claimsConnector.getClaimInformation(caseNumber, serviceType, None),
                                NotFound(notFound()).withHeaders("X-Explanation" -> "CLAIM_INFORMATION_NOT_FOUND")
                              )
      files                <- liftF(uploadedFilesCache.retrieveCurrentlyUploadedFiles(caseNumber))
      successfullyUploaded <-
        liftF(
          fileSubmissionConnector
            .submitFileToCDFPay(claim.declarationId, claim.isEntryNumber, request.eori, serviceType, caseNumber, files)
        )
      result               <- liftF(clearData(successfullyUploaded, caseNumber))
    } yield result

    result.merge
  }

  private def clearData(successfulUpload: Boolean, caseNumber: String)(implicit
    request: IdentifierRequest[_]
  ): Future[Result] =
    if (successfulUpload) {
      for {
        _     <- uploadedFilesCache.removeRecord(caseNumber)
        _     <- uploadDocumentsConnector.wipeData()
        email <- dataStoreConnector.getEmail(request.eori)
      } yield email match {
        case Left(_)      => NotFound(notFound()).withHeaders("X-Explanation" -> "EMAIL_NOT_FOUND")
        case Right(email) => Ok(confirmation(caseNumber, email.value))
      }
    } else Future.successful(NotFound(notFound()).withHeaders("X-Explanation" -> "UNSUCCESSFUL_UPLOAD_TO_CDFPAY"))
}
