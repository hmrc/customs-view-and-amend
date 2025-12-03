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

import config.AppConfig
import models.company.CompanyInformationResponse
import models.email.{EmailResponse, EmailResponses, UndeliverableEmail, UnverifiedEmail}
import play.api.Logging
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataStoreConnector @Inject() (http: HttpClientV2, appConfig: AppConfig)(implicit
  executionContext: ExecutionContext
) extends Logging {

  def getEmail(eori: String)(implicit hc: HeaderCarrier): Future[Either[EmailResponses, Email]] = {
    val dataStoreEndpoint = appConfig.customsDataStore + s"/eori/verified-email-third-party"
    val body: JsValue     = Json.obj("eori" -> eori)
    http
      .post(URL(dataStoreEndpoint))
      .withBody(body)
      .execute[EmailResponse]
      .map {
        case EmailResponse(Some(address), _, None)  => Right(Email(address))
        case EmailResponse(Some(email), _, Some(_)) => Left(UndeliverableEmail(email))
        case _                                      => Left(UnverifiedEmail)
      }
      .recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
        Left(UnverifiedEmail)
      }
  }

  def getCompanyName(eori: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val dataStoreEndpoint = appConfig.customsDataStore + s"/eori/company-information-third-party"
    val body: JsValue     = Json.obj("eori" -> eori)
    http
      .post(URL(dataStoreEndpoint))
      .withBody(body)
      .execute[CompanyInformationResponse]
      .map(response => Some(response.name))
  }.recover { case _ => None }

}
