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

package controllers

import models.CaseType._
import models._
import models.responses.{C285, ProcedureDetail}
import org.mockito.Mockito
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.test.Helpers._
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ErrorNewTaxTypeCodeValidationControllerSpec extends SpecBase {

  "newTaxTypeCodeValidation" should {
    "return OK when case number exists in claims" in new Setup {
      running(app) {
        val request =
          fakeRequest(GET, routes.ErrorNewTaxTypeCodeValidationController.showError("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "redirect to error page when API returns 500" in new Setup {
      running(app) {
        val request =
          fakeRequest(GET, routes.ErrorNewTaxTypeCodeValidationController.showError("someOtherClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe SEE_OTHER
      }
    }

  }

  trait Setup extends SetupBase {

    val claimDetail: ClaimDetail = ClaimDetail(
      "caseNumber",
      NDRC,
      Some("DeclarationId"),
      Seq(ProcedureDetail(MRNNumber = "DeclarationId", mainDeclarationReference = true)),
      Seq.empty,
      Some("SomeLrn"),
      Some("GB746502538945"),
      InProgress,
      None,
      Some(C285),
      Some(Bulk),
      Some(LocalDate.now),
      None,
      Some("1200"),
      Some("Sarah Philips"),
      Some("sarah.philips@acmecorp.com")
    )

    val allClaims: AllClaims = AllClaims(
      pendingClaims = Seq.empty,
      inProgressClaims = Seq(
        InProgressClaim("MRN", "someClaim", NDRC, None, Some(LocalDate.of(2021, 2, 1)))
      ),
      closedClaims = Seq.empty
    )

    val app: Application = applicationWithMongoCache.build()

    Mockito
      .lenient()
      .when(mockClaimsConnector.getAllClaims(any)(any))
      .thenReturn(Future.successful(allClaims))
  }

}
