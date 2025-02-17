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
import models.*
import models.responses.{AllClaimsResponse, NDRCCase, SCTYCase, SpecificClaimResponse}
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ClaimsConnectorImpl])
trait ClaimsConnector {

  def getAllClaims(includeXiClaims: Boolean = false)(implicit hc: HeaderCarrier): Future[AllClaims]

  def getClaimInformation(caseNumber: String, serviceType: ServiceType, lrn: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Option[ClaimDetail]]]

}

@Singleton
class ClaimsConnectorImpl @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit
  executionContext: ExecutionContext
) extends ClaimsConnector
    with Logging {

  private val baseUrl             = appConfig.cdsReimbursementClaim
  private val getGbClaimsUrl      = s"$baseUrl/claims"
  private val getGbAndXiClaimsUrl = s"$baseUrl/claims?includeXiClaims=true"

  private def getSpecificClaimUrl(serviceType: ServiceType, caseNumber: String): URL =
    URL(s"$baseUrl/claims/$serviceType/$caseNumber")

  final def getAllClaims(includeXiClaims: Boolean = false)(implicit hc: HeaderCarrier): Future[AllClaims] =
    httpClient
      .get(if (includeXiClaims) URL(getGbAndXiClaimsUrl) else URL(getGbClaimsUrl))
      .execute[AllClaimsResponse]
      .map { claimsResponse =>
        val claims =
          claimsResponse.claims.ndrcClaims.map(_.toClaim) ++
            claimsResponse.claims.sctyClaims
              .map(_.toClaim)
        AllClaims(
          claims.collect { case e: PendingClaim => e },
          claims.collect { case e: InProgressClaim => e },
          claims.collect { case e: ClosedClaim => e }
        )
      }
      // $COVERAGE-OFF$
      .recoverWith { case e =>
        logger.error(s"Error while getting claims using CDFPay TPI01 request: $e")
        Future.failed(e)
      }
  // $COVERAGE-ON$

  final def getClaimInformation(caseNumber: String, serviceType: ServiceType, lrn: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Option[ClaimDetail]]] =
    httpClient
      .get(getSpecificClaimUrl(serviceType, caseNumber))
      .execute[SpecificClaimResponse]
      .map {
        case SpecificClaimResponse("NDRC", true, Some(e: NDRCCase), None) => Right(Some(e.toClaimDetail(lrn)))
        case SpecificClaimResponse("SCTY", true, None, Some(e: SCTYCase)) => Right(Some(e.toClaimDetail(lrn)))
        case SpecificClaimResponse(_, _, Some(_), Some(_))                =>
          logger.error(s"Both NDRC/SCTY claim returned for case number $caseNumber")
          Right(None)
        case _                                                            => Right(None)
      }
      .recover {
        case UpstreamErrorResponse(_, 500, _, _) =>
          Left("ERROR_HTTP_500")
        case _                                   =>
          Right(None)
      }
}
