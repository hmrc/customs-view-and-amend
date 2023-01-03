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

import models.{IdentifierRequest, SessionData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ActionTransformer
import repositories.SessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CurrentSessionAction @Inject(
) (sessionCache: SessionCache)(implicit val executionContext: ExecutionContext, val messagesApi: MessagesApi)
    extends ActionTransformer[IdentifierRequest, CurrentSessionAction.RequestWithSessionData]
    with I18nSupport {

  override def transform[A](
    request: IdentifierRequest[A]
  ): Future[CurrentSessionAction.RequestWithSessionData[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    sessionCache
      .get()
      .flatMap(
        _.fold(
          error => Future.failed(error.exception),
          {
            case None              =>
              val sessionData = SessionData()
              sessionCache
                .store(sessionData)
                .flatMap(
                  _.fold(
                    error => Future.failed(error.exception),
                    _ => Future.successful((request, sessionData))
                  )
                )
            case Some(sessionData) => Future.successful((request, sessionData))
          }
        )
      )
  }
}

object CurrentSessionAction {
  type RequestWithSessionData[A] = (IdentifierRequest[A], SessionData)
}
