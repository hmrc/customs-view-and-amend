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
import models._
import models.requests.{ClaimsRequest, SpecificClaimRequest}
import models.responses.{AllClaimsResponse, SpecificClaimResponse}
import repositories.ClaimsCache
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialsApiConnector @Inject()(httpClient: HttpClient, claimsCache: ClaimsCache, appConfig: AppConfig)(
  implicit executionContext: ExecutionContext
) {

  private val baseUrl = appConfig.customsFinancialsApi
  private val getClaimsUrl = s"$baseUrl/get-claims"
  private val getSpecificClaimUrl = s"$baseUrl/get-specific-claim"

  def getClaims(eori: String)(implicit hc: HeaderCarrier): Future[AllClaims] = {
    for {
      cachedClaims <- claimsCache.get(eori)
      claims <- cachedClaims match {
        case Some(claims) =>
          Future.successful(claims)
        case None => httpClient.POST[ClaimsRequest, AllClaimsResponse](
          getClaimsUrl, ClaimsRequest(eori, "A")).flatMap { claimsResponse =>
          val claims = claimsResponse.claims.ndrcClaims.map(_.toNdrcClaim) ++ claimsResponse.claims.sctyClaims.map(_.toSctyClaim)
          claimsCache.set(eori, claims).map(_ => claims)
        }
      }
    } yield {
      AllClaims(
        claims.collect { case e: PendingClaim => e },
        claims.collect { case e: InProgressClaim => e },
        claims.collect { case e: ClosedClaim => e }
      )
    }
  }

  def getClaimInformation(caseNumber: String, claimType: ClaimType)(implicit hc: HeaderCarrier): Future[Option[ClaimDetail]] = {
    httpClient.POST[SpecificClaimRequest, SpecificClaimResponse](getSpecificClaimUrl, SpecificClaimRequest(claimType, caseNumber))
      .map(v => Some(v.toClaimDetail(claimType)))
      .recover {
        case _ => None
      }
  }
}
