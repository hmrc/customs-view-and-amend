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

import models._
import models.CaseType._
import models.email.UnverifiedEmail
import models.responses.{C285, ProcedureDetail}
import org.mockito.Mockito
import org.scalatest.matchers.must.Matchers._
import play.api.Application
import play.api.test.Helpers._
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimDetailControllerSpec extends SpecBase {

  "claimDetail" should {
    "return OK when a in progress claim has been found" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Right(Some(claimDetail))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK when a in progress claim has been found without claimType" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Right(Some(claimDetail.copy(claimType = None)))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK when a pending claim has been found" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Right(Some(claimDetail.copy(claimStatus = Pending)))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK when a closed claim has been found" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Right(Some(claimDetail.copy(claimStatus = Closed)))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return NOT_FOUND when user not authorised to view claim" in new Setup {
      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someOtherClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe NOT_FOUND
      }
    }

    "redirect to the unverified email page when there is no active email found" in new Setup {
      when(mockDataStoreConnector.getEmail(any)(any))
        .thenReturn(Future.successful(Left(UnverifiedEmail)))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe SEE_OTHER
      }
    }

    "return OK when there is email check connectivity issue" in new Setup {
      when(mockDataStoreConnector.getEmail(any)(any))
        .thenReturn(Future.failed(new Exception("email check fails")))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Right(Some(claimDetail))))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return NOT_FOUND when claim not found from the API" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Right(None)))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe NOT_FOUND
      }
    }

    "redirect to error page when API returns 500" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Left("ERROR_HTTP_500")))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          routes.ErrorNewTaxTypeCodeValidationController.showError("someClaim").url
        )
      }
    }

    "return NOT_FOUND when API returns other error" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Left("FOO")))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe NOT_FOUND
      }
    }

    "return NOT_FOUND when a claim is not present in the list of claims" in new Setup {
      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someOtherClaim").url)
        val result  = route(app, request).value
        status(result) shouldBe NOT_FOUND
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
