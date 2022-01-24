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
import play.api.libs.json.{Format, Json, OFormat}
import repositories.ClaimsCache
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialsApiConnector @Inject()(httpClient: HttpClient, claimsCache: ClaimsCache, appConfig: AppConfig)(
  implicit executionContext: ExecutionContext
) {

  private val baseUrl = appConfig.customsFinancialsApi
  private val getClaimsUrl = s"$baseUrl/get-claims"
  private val getSpecificClaimUrl = s"$baseUrl/get-specific-claims"

  case class ClaimsRequest(eori: String)

  object ClaimsRequest {
    implicit val format: Format[ClaimsRequest] = Json.format[ClaimsRequest]
  }

  case class SpecificClaimRequest(cdfPayService: String, cdfPayCaseNumber: String)

  object SpecificClaimRequest {
    implicit val format: Format[SpecificClaimRequest] = Json.format[SpecificClaimRequest]
  }

  case class CDFPayCaseDetail(CDFPayCaseNumber: String,
                              CDFPayService: String,
                              caseStatus: String,
                              declarantEORI: String,
                              importerEORI: String,
                              claimantEORI: Option[String],
                              claimAmountTotal: Option[String],
                              totalCaseReimburseAmnt: Option[String]) {

    def toClaim: Claim =
      caseStatus match {
        case "Open" => InProgressClaim(CDFPayCaseNumber, LocalDate.of(2000, 1, 1))
        case "Pending Decision Letter" => PendingClaim(CDFPayCaseNumber, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1))
        case "Resolved-Approved" => ClosedClaim(CDFPayCaseNumber, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 2, 1))
        case _ => throw new RuntimeException("Unhandled error")
      }
  }

  object CDFPayCaseDetail {
    implicit val format: OFormat[CDFPayCaseDetail] = Json.format[CDFPayCaseDetail]
  }

  case class ClaimsResponse(claims: Seq[CDFPayCaseDetail])

  object ClaimsResponse {
    implicit val format: OFormat[ClaimsResponse] = Json.format[ClaimsResponse]
  }

  def getClaims(eori: String)(implicit hc: HeaderCarrier): Future[AllClaims] = {
    httpClient.POST[ClaimsRequest, ClaimsResponse](getClaimsUrl, ClaimsRequest(eori)).map { response =>
      val claims: Seq[Claim] = response.claims.map(_.toClaim)
      AllClaims(
        claims.collect { case e: PendingClaim => e },
        claims.collect { case e: InProgressClaim => e },
        claims.collect { case e: ClosedClaim => e }
      )
    }
  }

  //TODO handle error cases from API (maybe use eithers)
  def getClaimInformation(caseNumber: String): Future[ClaimDetail] = Future.successful(
    ClaimDetail(
      caseNumber,
      Seq("21GB03I52858073821"),
      "GB746502538945",
      InProgress,
      C285,
      LocalDate.of(2021, 10, 23),
      1200,
      "Sarah Philips",
      "sarah.philips@acmecorp.com"
    ))
}
