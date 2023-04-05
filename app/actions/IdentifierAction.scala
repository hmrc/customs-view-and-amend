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

import com.google.inject.ImplementedBy
import config.AppConfig
import controllers.routes
import models.AuthorisedRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthenticatedIdentifierAction])
trait IdentifierAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with ActionFunction[Request, AuthorisedRequest]

@Singleton
class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions {
  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.allEnrolments) { allEnrolments =>
      allEnrolments
        .getEnrolment("HMRC-CUS-ORG")
        .flatMap(_.getIdentifier("EORINumber"))
      match {
        case Some(eori) =>
          block(AuthorisedRequest(request, eori.value))

        case None =>
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad))
      }
    } recover {
      case _: NoActiveSession        =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: InsufficientEnrolments =>
        Redirect(routes.UnauthorisedController.onPageLoad)
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad)
    }
  }
}
