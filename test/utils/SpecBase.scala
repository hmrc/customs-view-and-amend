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

package utils

import actions.IdentifierAction
import akka.stream.testkit.NoMaterializer
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import connector.DataStoreConnector
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.stubPlayBodyParsers
import uk.gov.hmrc.auth.core.retrieve.Email

import scala.concurrent.Future

class FakeMetrics extends Metrics {
  override val defaultRegistry: MetricRegistry = new MetricRegistry
  override val toJson: String = "{}"
}

trait SpecBase extends AnyWordSpecLike with MockitoSugar with OptionValues with ScalaFutures with Matchers with IntegrationPatience {

  def fakeRequest(method: String = "", path: String = ""): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
      .withHeaders("X-Session-Id" -> "someSessionId")

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest("", ""))

  val mockDataStoreConnector: DataStoreConnector = mock[DataStoreConnector]

  when(mockDataStoreConnector.getEmail(any)(any))
    .thenReturn(Future.successful(Right(Email("some@email.com"))))

  def application: GuiceApplicationBuilder = new GuiceApplicationBuilder().overrides(
    bind[IdentifierAction].toInstance(new FakeIdentifierAction(stubPlayBodyParsers(NoMaterializer))),
    bind[DataStoreConnector].toInstance(mockDataStoreConnector),
    bind[Metrics].toInstance(new FakeMetrics)
  ).configure(
    "play.filters.csp.nonce.enabled" -> "false",
    "auditing.enabled" -> "false",
    "metrics.enabled" -> "false"
  )

}
