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

package connector

import com.google.inject.ImplementedBy
import config.AppConfig
import models._
import models.file_upload.UploadedFile
import play.api.Logging
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[FileSubmissionConnectorImpl])
trait FileSubmissionConnector {

  def submitFileToCDFPay(
    declarationId: String,
    entryNumber: Boolean,
    eori: String,
    serviceType: ServiceType,
    caseNumber: String,
    files: Seq[UploadedFile],
    reasonForSecurity: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean]

}

@Singleton
class FileSubmissionConnectorImpl @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit
  executionContext: ExecutionContext
) extends FileSubmissionConnector
    with Logging {

  private val baseUrl       = appConfig.cdsReimbursementClaim
  private val fileUploadUrl = s"$baseUrl/claims/files"

  final def submitFileToCDFPay(
    declarationId: String,
    entryNumber: Boolean,
    eori: String,
    serviceType: ServiceType,
    caseNumber: String,
    files: Seq[UploadedFile],
    reasonForSecurity: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean] = {
    val request = Dec64UploadRequest(
      UUID.randomUUID().toString,
      eori,
      caseNumber,
      declarationId,
      entryNumber,
      serviceType.dec64ServiceType,
      files.map(_.toDec64UploadedFile),
      reasonForSecurity
    )
    httpClient
      .POST[Dec64UploadRequest, HttpResponse](fileUploadUrl, request)
      .map {
        case HttpResponse(ACCEPTED, _, _) => true

        case failedResponse =>
          logger.error(
            s"Submitting files for $caseNumber to CDFPay has failed with ${failedResponse.status} status:\n${failedResponse.body}"
          )
          false
      }
      .recover { case exception =>
        logger.error(s"Submitting files for $caseNumber to CDFPay resulted in an error: $exception")
        false
      }
  }
}
