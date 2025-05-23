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
import controllers.routes
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.BodyParsers.Default

class IdentifierActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  "IdentifierAction" when {
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
          new AuthenticatedIdentifierAction(mockAuthConnector, config, bodyParsers)
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
          new AuthenticatedIdentifierAction(mockAuthConnector, config, bodyParsers)
        val controller = new Harness(authAction)

        running(app) {
          val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
          status(result) shouldBe SEE_OTHER
        }
      }
    }

    "the user hasn't logged in" should {

      "redirect the user to log in " in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new MissingBearerToken),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should startWith(config.loginUrl)
      }
    }

    "the user's session has expired" should {

      "redirect the user to log in " in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new BearerTokenExpired),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe SEE_OTHER

        redirectLocation(result).get should startWith(config.loginUrl)
      }
    }

    "the user doesn't have sufficient enrolments" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new InsufficientEnrolments),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "the user doesn't have sufficient confidence level" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "the user used an unaccepted auth provider" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new UnsupportedAuthProvider),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "the user has an unsupported affinity group" should {

      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "the user has an unsupported credential role" should {
      "redirect the user to the unauthorised page" in new SetupBase {

        val app    = application.build()
        val config = app.injector.instanceOf[AppConfig]

        val bodyParsers = application.injector().instanceOf[BodyParsers.Default]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new UnsupportedCredentialRole),
          config,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest())

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "the user is on the limited access list" should {
      "call block" in new SetupBase {
        val mockAuthConnector: AuthConnector & scala.reflect.Selectable = mock[AuthConnector]

        (mockAuthConnector
          .authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(
            Future.successful(
              Enrolments(
                Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("EORINumber", "GB000000000000001")), "Active"))
              )
            )
          )

        val app = application
          .configure(
            "features.limited-access"        -> "true",
            "limited-access-eori-csv-base64" -> "R0IwMDAwMDAwMDAwMDAwMDEsR0IwMDAwMDAwMDAwMDAwMDIK"
          )
          .overrides()
          .build()

        val config      = app.injector.instanceOf[AppConfig]
        val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

        val authAction =
          new AuthenticatedIdentifierAction(mockAuthConnector, config, bodyParsers)
        val controller = new Harness(authAction)

        running(app) {
          val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
          status(result) shouldBe OK
        }
      }
    }

    "the user is NOT on the limited access list" should {
      "redirect the user to the unauthorised page" in new SetupBase {
        val mockAuthConnector: AuthConnector & scala.reflect.Selectable = mock[AuthConnector]

        (mockAuthConnector
          .authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(
            Future.successful(
              Enrolments(
                Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("EORINumber", "GB000000000000003")), "Active"))
              )
            )
          )

        val app = application
          .configure(
            "features.limited-access"        -> "true",
            "limited-access-eori-csv-base64" -> "R0IwMDAwMDAwMDAwMDAwMDEsR0IwMDAwMDAwMDAwMDAwMDIK"
          )
          .overrides()
          .build()

        val config      = app.injector.instanceOf[AppConfig]
        val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

        val authAction =
          new AuthenticatedIdentifierAction(mockAuthConnector, config, bodyParsers)
        val controller = new Harness(authAction)

        running(app) {
          val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
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
