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

package services

import connector.{ClaimsConnector, UploadDocumentsConnector}
import repositories.{ClaimsCache, ClaimsMongo, UploadedFilesCache}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimService @Inject() (
  claimsConnector: ClaimsConnector,
  uploadDocumentsConnector: UploadDocumentsConnector,
  uploadedFilesCache: UploadedFilesCache,
  claimsCache: ClaimsCache
)(implicit executionContext: ExecutionContext) {

  def authorisedToView(caseNumber: String, eori: String)(implicit hc: HeaderCarrier): Future[Option[ClaimsMongo]] =
    for {
      _      <- claimsConnector.getClaims(eori)
      result <- claimsCache.getSpecificCase(eori, caseNumber)
    } yield result

  def clearUploaded(caseNumber: String, initialRequest: Boolean)(implicit hc: HeaderCarrier): Future[Unit] =
    if (initialRequest) {
      for {
        _ <- uploadedFilesCache.removeRecord(caseNumber)
        _ <- uploadDocumentsConnector.wipeData()
      } yield ()
    } else { Future.unit }
}
