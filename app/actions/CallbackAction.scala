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
import models.AuthorisedRequest
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthorisedCallbackAction])
trait CallbackAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with ActionFunction[Request, AuthorisedRequest]

@Singleton
class AuthorisedCallbackAction @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends CallbackAction
    with AuthorisedFunctions {
  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]) = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    authorised().retrieve(Retrievals.allEnrolments) { allEnrolments =>
      allEnrolments
        .getEnrolment("HMRC-CUS-ORG")
        .flatMap(_.getIdentifier("EORINumber")) match {
        case Some(eori) =>
          block(AuthorisedRequest(request, eori.value, isCallback = true))

        case None =>
          Future.successful(Forbidden)
      }
    } recover { case _ =>
      Forbidden
    }
  }
}
