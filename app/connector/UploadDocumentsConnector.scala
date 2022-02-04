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
import models.ClaimType
import models.file_upload.{Nonce, UploadDocumentsWrapper}
import play.api.http.Status.CREATED
import repositories.UploadedFilesCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadDocumentsConnector @Inject()(httpClient: HttpClient,
                                         uploadedFilesCache: UploadedFilesCache
                                        )(implicit executionContext: ExecutionContext, appConfig: AppConfig) {



  def initializeNewFileUpload(caseNumber: String, claimType: ClaimType, searched: Boolean, multipleUpload: Boolean)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val nonce = Nonce.random
    for {
      successfulWrite <- uploadedFilesCache.initializeRecord(caseNumber, nonce)
      payload = UploadDocumentsWrapper.createPayload(nonce, caseNumber, claimType, searched, multipleUpload)
      result <- if (successfulWrite) { sendRequest(payload) } else Future.successful(None)
    } yield result
  }

  private def sendRequest(uploadDocumentsWrapper: UploadDocumentsWrapper)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpClient.POST[UploadDocumentsWrapper, HttpResponse](appConfig.fileUploadInitializeUrl, uploadDocumentsWrapper).map { response =>
      response.status match {
        case CREATED => response.header("Location")
        case _ => None
      }
    }.recover { case _ => None }
  }


}
