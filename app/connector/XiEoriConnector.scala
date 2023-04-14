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

import cats.implicits.catsSyntaxEq
import com.google.inject.ImplementedBy
import config.AppConfig
import models.XiEori
import play.api.Logging
import play.api.http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[XiEoriConnectorImpl])
trait XiEoriConnector {
  def getXiEori(implicit hc: HeaderCarrier): Future[Option[XiEori]]
}

@Singleton
class XiEoriConnectorImpl @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit
  executionContext: ExecutionContext
) extends XiEoriConnector
    with Logging {
  private val baseUrl      = appConfig.cdsReimbursementClaim
  private val getXiEoriUrl = s"$baseUrl/eori/xi"

  final def getXiEori(implicit hc: HeaderCarrier): Future[Option[XiEori]] =
    httpClient
      .GET[HttpResponse](getXiEoriUrl)
      .flatMap { response =>
        if (response.status === OK) {
          Future(response.json.asOpt[XiEori])
        } else if (response.status === NO_CONTENT) {
          Future.successful(None)
        } else {
          Future.failed(
            new XiEoriConnector.Exception(s"call to get XI EORI details has failed: $response")
          )
        }
      }
}

object XiEoriConnector {
  class Exception(message: String) extends scala.Exception(message)
}
