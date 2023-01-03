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

package utils

import akka.stream.testkit.NoMaterializer
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import connector.{ClaimsConnector, DataStoreConnector}
import models.CaseType.Individual
import models.Reimbursement
import models.responses.{C285, EntryDetail, Goods, NDRCAmounts, NDRCCase, NDRCDetail, ProcedureDetail, SCTYCase}
import org.mockito.Mockito
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
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.stubPlayBodyParsers
import repositories.SessionCache
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

class FakeMetrics extends Metrics {
  override val defaultRegistry: MetricRegistry = new MetricRegistry
  override val toJson: String                  = "{}"
}

trait SpecBase
    extends AnyWordSpecLike
    with MockitoSugar
    with OptionValues
    with ScalaFutures
    with Matchers
    with IntegrationPatience {

  def fakeRequest(method: String = "", path: String = "")(implicit
    hc: HeaderCarrier = HeaderCarrier()
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withCSRFToken
      .asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
      .withHeaders(
        "X-Session-Id" ->
          hc.sessionId.map(_.value).getOrElse(UUID.randomUUID().toString())
      )

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest("", ""))

  trait SetupBase {

    val mockDataStoreConnector: DataStoreConnector = mock[DataStoreConnector]
    val mockSessionCache: SessionCache             = mock[SessionCache]
    val mockClaimsConnector: ClaimsConnector       = mock[ClaimsConnector]

    Mockito
      .lenient()
      .when(mockDataStoreConnector.getEmail(any)(any))
      .thenReturn(Future.successful(Right(Email("some@email.com"))))

    Mockito
      .lenient()
      .when(mockDataStoreConnector.getCompanyName(any)(any))
      .thenReturn(Future.successful(Some("companyName")))

    val reimbursement: Reimbursement = Reimbursement("date", "10.00", "10.00", "method")

    val ndrcCase: NDRCCase = NDRCCase(
      NDRCDetail(
        CDFPayCaseNumber = "CaseNumber",
        declarationID = "DeclarationId",
        claimType = C285,
        caseType = Individual,
        caseStatus = "Closed",
        caseSubStatus = Some("Refused"),
        descOfGoods = Some("description of goods"),
        descOfRejectedGoods = Some("description of rejected goods"),
        declarantEORI = "SomeEori",
        importerEORI = "SomeOtherEori",
        claimantEORI = Some("ClaimaintEori"),
        basisOfClaim = Some("basis of claim"),
        claimStartDate = "20221012",
        claimantName = Some("name"),
        claimantEmailAddress = Some("email@email.com"),
        closedDate = Some("20221112"),
        MRNDetails = Some(
          Seq(
            ProcedureDetail("MRN", true)
          )
        ),
        entryDetails = Some(
          Seq(
            EntryDetail("entryNumber", true)
          )
        ),
        reimbursement = Some(Seq(reimbursement))
      ),
      NDRCAmounts(
        Some("600000"),
        Some("600000"),
        Some("600000"),
        Some("600000"),
        Some("600000"),
        Some("600000"),
        Some("600000"),
        Some("600000"),
        Some("600000")
      )
    )
    val sctyCase: SCTYCase = SCTYCase(
      "caseNumber",
      "declarationId",
      "Reason for security",
      "Procedure Code",
      "Closed",
      Some("Refused"),
      None,
      Some(Seq(Goods("itemNumber", Some("description")))),
      "someEori",
      "someOtherEori",
      Some("claimantEori"),
      Some("600000"),
      Some("600000"),
      Some("600000"),
      Some("600000"),
      "20221210",
      Some("name"),
      Some("email@email.com"),
      Some("20221012"),
      Some(Seq(reimbursement))
    )

    def application: GuiceApplicationBuilder = new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(new FakeAuthConector(stubPlayBodyParsers(NoMaterializer))),
        bind[DataStoreConnector].toInstance(mockDataStoreConnector),
        bind[SessionCache].toInstance(mockSessionCache),
        bind[ClaimsConnector].toInstance(mockClaimsConnector),
        bind[Metrics].toInstance(new FakeMetrics)
      )
      .configure(
        "play.filters.csp.nonce.enabled" -> "false",
        "auditing.enabled"               -> "false",
        "metrics.enabled"                -> "false",
        "session-store.expiry-time"      -> "15 seconds"
      )

    def applicationWithMongoCache: GuiceApplicationBuilder = new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(new FakeAuthConector(stubPlayBodyParsers(NoMaterializer))),
        bind[DataStoreConnector].toInstance(mockDataStoreConnector),
        bind[ClaimsConnector].toInstance(mockClaimsConnector),
        bind[Metrics].toInstance(new FakeMetrics)
      )
      .configure(
        "play.filters.csp.nonce.enabled" -> "false",
        "auditing.enabled"               -> "false",
        "metrics.enabled"                -> "false",
        "session-store.expiry-time"      -> "15 seconds"
      )
  }

}
