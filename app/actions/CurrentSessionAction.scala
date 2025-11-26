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
import connector.DataStoreConnector
import models.email.{UndeliverableEmail, UnverifiedEmail}
import models.{AuthorisedRequest, AuthorisedRequestWithSessionData, SessionData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.*
import play.api.mvc.{ActionRefiner, Result}
import repositories.SessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.email.undeliverable_email

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CurrentSessionAction @Inject(
) (
  sessionCache: SessionCache,
  connector: DataStoreConnector,
  undeliverableEmail: undeliverable_email,
  appConfig: AppConfig
)(implicit
  val executionContext: ExecutionContext,
  val messagesApi: MessagesApi
) extends ActionRefiner[AuthorisedRequest, AuthorisedRequestWithSessionData]
    with I18nSupport {

  override def refine[A](
    request: AuthorisedRequest[A]
  ): Future[Either[Result, AuthorisedRequestWithSessionData[A]]] = {
    implicit val hc: HeaderCarrier =
      if (request.isCallback)
        HeaderCarrierConverter.fromRequest(request)
      else
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    sessionCache
      .get()
      .flatMap(
        _.fold(
          error => Future.failed(error.exception),
          {
            case None =>
              retrieveVerifiedEmail(request, SessionData())
                .flatMap(
                  _.fold(
                    x => Future.successful(Left(x)),
                    retrieveCompanyName(request, _)
                      .flatMap { sessionData =>
                        sessionCache
                          .store(sessionData)
                          .flatMap(
                            _.fold(
                              error => Future.failed(error.exception),
                              _ => Future.successful(Right(request.withSessionData(sessionData)))
                            )
                          )
                      }
                  )
                )

            case Some(sessionData) =>
              Future.successful(Right(request.withSessionData(sessionData)))
          }
        )
      )
  }

  private def retrieveVerifiedEmail[A](
    request: AuthorisedRequest[A],
    sessionData: SessionData
  )(implicit hc: HeaderCarrier): Future[Either[Result, SessionData]] =
    connector
      .getEmail()
      .map {
        case Left(value) =>
          value match {
            case UndeliverableEmail(_) =>
              Left(Ok(undeliverableEmail(appConfig.emailFrontendUrl)(request, request.messages, appConfig)))

            case UnverifiedEmail =>
              Left(Redirect(controllers.routes.EmailController.showUnverified()))
          }

        case Right(email) =>
          Right(sessionData.withVerifiedEmail(email.value))
      }
      .recover { case _ =>
        // This will allow users to access the service if ETMP return an error via SUB09
        Right(sessionData)
      }

  private def retrieveCompanyName[A](
    request: AuthorisedRequest[A],
    sessionData: SessionData
  )(implicit hc: HeaderCarrier): Future[SessionData] =
    connector
      .getCompanyName()
      .map {
        case Some(companyName) =>
          sessionData.withCompanyName(companyName)

        case None =>
          sessionData
      }
      .recover { case _ =>
        // This will allow users to access the service if ETMP return an error via SUB09
        sessionData
      }
}
