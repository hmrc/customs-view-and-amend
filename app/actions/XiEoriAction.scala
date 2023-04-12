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

package actions

import connector.XiEoriConnector
import models.AuthorisedRequestWithSessionData
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ActionTransformer
import repositories.SessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import config.AppConfig

@Singleton
class XiEoriAction @Inject(
) (
  sessionCache: SessionCache,
  xiEoriConnector: XiEoriConnector,
  appConfig: AppConfig
)(implicit
  val executionContext: ExecutionContext,
  val messagesApi: MessagesApi
) extends ActionTransformer[AuthorisedRequestWithSessionData, AuthorisedRequestWithSessionData]
    with I18nSupport {

  override def transform[A](
    request: AuthorisedRequestWithSessionData[A]
  ): Future[AuthorisedRequestWithSessionData[A]] =
    if (appConfig.includeXiClaims) {
      request.sessionData.xiEori match {
        case Left(()) =>
          getAndStoreXiEori(request)
        case Right(_) =>
          Future.successful(request)
      }
    } else
      Future.successful(request)

  private def getAndStoreXiEori[A](
    request: AuthorisedRequestWithSessionData[A]
  ): Future[AuthorisedRequestWithSessionData[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    xiEoriConnector.getXiEori
      .flatMap { case xiEori =>
        sessionCache
          .store(request.sessionData.withXiEori(xiEori))
          .flatMap(
            _.fold(
              error => Future.failed(error.exception),
              _ => Future.successful(request.withXiEori(xiEori))
            )
          )
      }
  }
}
