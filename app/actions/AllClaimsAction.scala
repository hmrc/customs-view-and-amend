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

import models.{AuthorisedRequest, SessionData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ActionTransformer
import repositories.SessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import models.AllClaims
import connector.ClaimsConnector
import scala.util.control.NonFatal
import uk.gov.hmrc.auth.core.SessionRecordNotFound

@Singleton
class AllClaimsAction @Inject(
) (
  sessionCache: SessionCache,
  claimsConnector: ClaimsConnector
)(implicit
  val executionContext: ExecutionContext,
  val messagesApi: MessagesApi
) extends ActionTransformer[AuthorisedRequest, AllClaimsAction.RequestWithClaims]
    with I18nSupport {

  override def transform[A](
    request: AuthorisedRequest[A]
  ): Future[AllClaimsAction.RequestWithClaims[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    sessionCache
      .get()
      .recoverWith { case NonFatal(e) =>
        Future.failed(new SessionRecordNotFound)
      }
      .flatMap(
        _.fold(
          error => Future.failed(error.exception),
          {
            case None =>
              getAndStoreAllClaims
                .map(allClaims => (request, allClaims))

            case Some(sessionData) =>
              sessionData.claims match {
                case None =>
                  getAndStoreAllClaims
                    .map(allClaims => (request, allClaims))

                case Some(allClaims) =>
                  Future.successful((request, allClaims))
              }
          }
        )
      )
  }

  private def getAndStoreAllClaims(implicit hc: HeaderCarrier): Future[AllClaims] =
    claimsConnector.getAllClaims
      .flatMap { allClaims =>
        val sessionData = SessionData(Some(allClaims))
        sessionCache
          .store(sessionData)
          .flatMap(
            _.fold(
              error => Future.failed(AllClaimsAction.ClaimsNotFoundException(error.exception)),
              _ => Future.successful(allClaims)
            )
          )
      }
      .recoverWith { case NonFatal(e) =>
        Future.failed(AllClaimsAction.ClaimsNotFoundException(e))
      }
}

object AllClaimsAction {

  case class ClaimsNotFoundException(cause: Throwable) extends Exception(cause)

  type RequestWithClaims[A] = (AuthorisedRequest[A], AllClaims)
}
