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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
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
        val mockAuthConnector = mock[AuthConnector]

        when(mockDataStoreConnector.getCompanyName(any)(any))
          .thenReturn(Future.successful(None))

        when(mockAuthConnector.authorise[Enrolments](any, any)(any, any))
          .thenReturn(
            Future.successful(
              Enrolments(Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("EORINumber", "test")), "Active")))
            )
          )

        val app         = application.overrides().build()
        val config      = app.injector.instanceOf[AppConfig]
        val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

        val authAction =
          new AuthorisedCallbackAction(mockAuthConnector, config, bodyParsers)
        val controller = new Harness(authAction)

        running(app) {
          val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
          status(result) mustBe OK
        }
      }
    }

    "the user does not have the correct enrolment" should {
      "return unauthorised" in new SetupBase {
        val mockAuthConnector = mock[AuthConnector]

        when(mockAuthConnector.authorise[Enrolments](any, any)(any, any))
          .thenReturn(
            Future.successful(
              Enrolments(Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("INVALID", "test")), "Active")))
            )
          )

        val app         = application.overrides().build()
        val config      = app.injector.instanceOf[AppConfig]
        val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

        val authAction =
          new AuthorisedCallbackAction(mockAuthConnector, config, bodyParsers)
        val controller = new Harness(authAction)

        running(app) {
          val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
          status(result) mustBe FORBIDDEN
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
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) mustBe FORBIDDEN
      }
    }

    "the user's session has expired" should {

      "redirect the user to log in " in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new BearerTokenExpired),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) mustBe FORBIDDEN
      }
    }

    "the user doesn't have sufficient enrolments" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new InsufficientEnrolments),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) mustBe FORBIDDEN
      }
    }

    "the user doesn't have sufficient confidence level" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) mustBe FORBIDDEN
      }
    }

    "the user used an unaccepted auth provider" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new UnsupportedAuthProvider),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) mustBe FORBIDDEN
      }
    }

    "the user has an unsupported affinity group" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) mustBe FORBIDDEN
      }
    }

    "the user has an unsupported credential role" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthorisedCallbackAction(
          new FakeFailingAuthConnector(new UnsupportedCredentialRole),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) mustBe FORBIDDEN
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
