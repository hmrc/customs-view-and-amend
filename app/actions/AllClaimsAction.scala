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

import config.AppConfig
import connector.ClaimsConnector
import models.{AllClaims, AuthorisedRequestWithSessionData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ActionTransformer
import repositories.SessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AllClaimsAction @Inject(
) (
  sessionCache: SessionCache,
  claimsConnector: ClaimsConnector,
  appConfig: AppConfig
)(implicit
  val executionContext: ExecutionContext,
  val messagesApi: MessagesApi
) extends ActionTransformer[AuthorisedRequestWithSessionData, AllClaimsAction.RequestWithClaims]
    with I18nSupport {

  override def transform[A](
    request: AuthorisedRequestWithSessionData[A]
  ): Future[AllClaimsAction.RequestWithClaims[A]] =
    request.sessionData.claims match {
      case None =>
        getAndStoreAllClaims(request)

      case Some(allClaims) =>
        Future.successful((request, allClaims))
    }

  private def getAndStoreAllClaims[A](
    request: AuthorisedRequestWithSessionData[A]
  ): Future[AllClaimsAction.RequestWithClaims[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    claimsConnector
      .getAllClaims(appConfig.includeXiClaims)
      .flatMap { allClaims =>
        sessionCache
          .store(request.sessionData.withAllClaims(allClaims))
          .flatMap(
            _.fold(
              error => Future.failed(AllClaimsAction.ClaimsNotFoundException(error.exception)),
              _ => Future.successful((request.withAllClaims(allClaims), allClaims))
            )
          )
      }
      .recoverWith { case NonFatal(e) =>
        Future.failed(AllClaimsAction.ClaimsNotFoundException(e))
      }
  }
}

object AllClaimsAction {

  case class ClaimsNotFoundException(cause: Throwable) extends Exception(cause)

  type RequestWithClaims[A] = (AuthorisedRequestWithSessionData[A], AllClaims)
}
