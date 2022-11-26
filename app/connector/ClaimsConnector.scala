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

import com.google.inject.ImplementedBy
import config.AppConfig
import models._
import models.responses.{AllClaimsResponse, NDRCCase, SCTYCase, SpecificClaimResponse}
import play.api.Logging
import repositories.ClaimsCache
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ClaimsConnectorImpl])
trait ClaimsConnector {

  def getClaims(eori: String)(implicit hc: HeaderCarrier): Future[AllClaims]

  def getClaimInformation(caseNumber: String, serviceType: ServiceType, lrn: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Option[ClaimDetail]]

}

@Singleton
class ClaimsConnectorImpl @Inject() (httpClient: HttpClient, claimsCache: ClaimsCache, appConfig: AppConfig)(implicit
  executionContext: ExecutionContext
) extends ClaimsConnector
    with Logging {

  private val baseUrl      = appConfig.cdsReimbursementClaim
  private val getClaimsUrl = s"$baseUrl/claims"

  private def getSpecificClaimUrl(serviceType: ServiceType, caseNumber: String) =
    s"$baseUrl/claims/$serviceType/$caseNumber"

  final def getClaims(eori: String)(implicit hc: HeaderCarrier): Future[AllClaims] =
    for {
      cachedClaims <- claimsCache.get(eori)
      claims       <- cachedClaims match {
                        case Some(claims) =>
                          Future.successful(claims)
                        case None         =>
                          httpClient
                            .GET[AllClaimsResponse](getClaimsUrl)
                            .flatMap { claimsResponse =>
                              val claims =
                                claimsResponse.claims.ndrcClaims.map(_.toClaim) ++
                                  claimsResponse.claims.sctyClaims
                                    .map(_.toClaim)
                              claimsCache.set(eori, claims).map(_ => claims)
                            }
                      }
    } yield AllClaims(
      claims.collect { case e: PendingClaim => e },
      claims.collect { case e: InProgressClaim => e },
      claims.collect { case e: ClosedClaim => e }
    )

  final def getClaimInformation(caseNumber: String, serviceType: ServiceType, lrn: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Option[ClaimDetail]] =
    httpClient
      .GET[SpecificClaimResponse](
        getSpecificClaimUrl(serviceType, caseNumber)
      )
      .map {
        case SpecificClaimResponse("NDRC", true, Some(e: NDRCCase), None) => Some(e.toClaimDetail(lrn))
        case SpecificClaimResponse("SCTY", true, None, Some(e: SCTYCase)) => Some(e.toClaimDetail(lrn))
        case SpecificClaimResponse(_, _, Some(_), Some(_))                =>
          logger.error(s"Both NDRC/SCTY claim returned for case number $caseNumber")
          None
        case _                                                            => None
      }
      .recover { case _ =>
        None
      }
}
