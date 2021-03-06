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

package actions

import config.AppConfig
import connector.DataStoreConnector
import models.IdentifierRequest
import models.email.{UndeliverableEmail, UnverifiedEmail}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.email.undeliverable_email

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailAction @Inject()(connector: DataStoreConnector,
                            undeliverableEmail: undeliverable_email,
                            appConfig: AppConfig)(implicit val executionContext: ExecutionContext, val messagesApi: MessagesApi) extends ActionFilter[IdentifierRequest] with I18nSupport {
  def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    connector.getEmail(request.eori).map {
      case Left(value) =>
        value match {
          case UndeliverableEmail(_) => Some(Ok(undeliverableEmail(appConfig.emailFrontendUrl)(request, request.messages , appConfig)))
          case UnverifiedEmail => Some(Redirect(controllers.routes.EmailController.showUnverified()))
        }
      case Right(_) => None
    }.recover { case _ => None } //This will allow users to access the service if ETMP return an error via SUB09
  }
}

