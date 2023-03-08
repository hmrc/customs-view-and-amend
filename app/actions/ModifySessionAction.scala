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

import models.{AuthorisedRequestWithSessionData, SessionData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ActionTransformer
import repositories.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ModifySessionAction @Inject(
) (sessionCache: SessionCache)(implicit val executionContext: ExecutionContext, val messagesApi: MessagesApi)
    extends ActionTransformer[AuthorisedRequestWithSessionData, ModifySessionAction.RequestWithSessionModifier]
    with I18nSupport {

  override def transform[A](
    request: AuthorisedRequestWithSessionData[A]
  ): Future[ModifySessionAction.RequestWithSessionModifier[A]] =
    Future.successful((request, new ModifySessionAction.SessionModifier(sessionCache, request.sessionData)))
}

object ModifySessionAction {

  final class SessionModifier(sessionCache: SessionCache, val current: SessionData) {
    def update(
      f: SessionData => SessionData
    )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SessionData]] =
      sessionCache
        .update(f)
        .transform {
          case Success(Right(modifiedSessionData)) => Success(Some(modifiedSessionData))
          case Success(Left(error))                => Failure(error.exception)
          case Failure(error)                      => Failure(error)
        }
  }

  type RequestWithSessionModifier[A] = (AuthorisedRequestWithSessionData[A], SessionModifier)
}
