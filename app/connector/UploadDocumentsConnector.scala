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

package connector

import config.AppConfig
import models.{FileSelection, ServiceType}
import models.file_upload.{Nonce, UploadDocumentsWrapper}
import models.responses.ClaimType
import play.api.Logging
import play.api.http.Status.{CREATED, NO_CONTENT}
import play.api.i18n.Messages
import repositories.UploadedFilesCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadDocumentsConnector @Inject() (httpClient: HttpClient, uploadedFilesCache: UploadedFilesCache)(implicit
  executionContext: ExecutionContext,
  appConfig: AppConfig
) extends Logging {

  def startFileUpload(caseNumber: String, claimType: ClaimType, serviceType: ServiceType, documentType: FileSelection)(
    implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Option[String]] = {
    val nonce = Nonce.random
    for {
      previouslyUploaded <- uploadedFilesCache.retrieveCurrentlyUploadedFiles(caseNumber)
      successfulWrite    <- uploadedFilesCache.initializeRecord(caseNumber, nonce, previouslyUploaded)
      payload             =
        UploadDocumentsWrapper
          .createPayload(nonce, caseNumber, serviceType, claimType, documentType, previouslyUploaded)
      result             <- if (successfulWrite) { sendRequest(payload) }
                            else {
                              logger.error(
                                s"Cannot initialize file upload:\nsuccessfulWrite = $successfulWrite\ninitalizationRequest = $payload"
                              )
                              Future.successful(None)
                            }
    } yield result
  }

  def wipeData()(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .POSTEmpty[HttpResponse](appConfig.fileUploadWipeOutUrl)
      .map(_.status == NO_CONTENT)
      .recover { case e =>
        logger.warn(s"Failed to wipe data in upload-document-frontend: $e")
        false
      }

  private def sendRequest(
    uploadDocumentsWrapper: UploadDocumentsWrapper
  )(implicit hc: HeaderCarrier): Future[Option[String]] =
    httpClient
      .POST[UploadDocumentsWrapper, HttpResponse](appConfig.fileUploadInitializationUrl, uploadDocumentsWrapper)
      .map { response =>
        response.status match {
          case CREATED => response.header("Location")
          case _       => None
        }
      }
      .recover { case _ => None }
}
