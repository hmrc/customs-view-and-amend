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

import com.google.inject.Inject
import config.AppConfig
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CallbackActionSpec extends SpecBase {

  class Harness(authAction: CallbackAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  "Callback Action" when {
    "successfully passes all checks" should {
      "call block" in new SetupBase {
        val mockAuthConnector: AuthConnector & scala.reflect.Selectable = mock[AuthConnector]

        (mockAuthConnector
          .authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(
            Future.successful(
              Enrolments(Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("EORINumber", "test")), "Active")))
            )
          )

        val app         = application.overrides().build()
        val config      = app.injector.instanceOf[AppConfig]
        val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

        val authAction =
          new AuthorisedCallbackAction(mockAuthConnector, bodyParsers)
        val controller = new Harness(authAction)

        running(app) {
          val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
          status(result) shouldBe OK
        }
      }
    }

    "the user does not have the correct enrolment" should {
      "return unauthorised" in new SetupBase {
        val mockAuthConnector: AuthConnector & scala.reflect.Selectable = mock[AuthConnector]

        (mockAuthConnector
          .authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(
            Future.successful(
              Enrolments(Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("INVALID", "test")), "Active")))
            )
          )

        val app         = application.overrides().build()
        val config      = app.injector.instanceOf[AppConfig]
        val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

        val authAction =
          new AuthorisedCallbackAction(mockAuthConnector, bodyParsers)
        val controller = new Harness(authAction)

        running(app) {
          val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
          status(result) shouldBe FORBIDDEN
        }
      }
    }

    "the user hasn't logged in" should {

      "redirect the user to log in " in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new MissingBearerToken),
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe FORBIDDEN
      }
    }

    "the user's session has expired" should {

      "redirect the user to log in " in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new BearerTokenExpired),
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe FORBIDDEN
      }
    }

    "the user doesn't have sufficient enrolments" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new InsufficientEnrolments),
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe FORBIDDEN
      }
    }

    "the user doesn't have sufficient confidence level" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe FORBIDDEN
      }
    }

    "the user used an unaccepted auth provider" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new UnsupportedAuthProvider),
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe FORBIDDEN
      }
    }

    "the user has an unsupported affinity group" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe FORBIDDEN
      }
    }

    "the user has an unsupported credential role" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new UnsupportedCredentialRole),
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe FORBIDDEN
      }
    }
  }

  class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] =
      Future.failed(exceptionToReturn)
  }
}
