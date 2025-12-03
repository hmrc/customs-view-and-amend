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

import models.*
import models.CaseType.*
import models.email.UnverifiedEmail
import models.responses.{C285, ProcedureDetail}
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class ClaimDetailControllerSpec extends SpecBase {

  "claimDetail" should {
    "return OK when an in progress claim has been found" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Right(Some(claimDetail))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-1234").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK when an in progress claim has been found without claimType" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Right(Some(claimDetail.copy(claimType = None)))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-1234").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK when a pending claim has been found" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Right(Some(claimDetail.copy(claimStatus = Pending)))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-1234").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK when a closed claim has been found" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Right(Some(claimDetail.copy(claimStatus = Closed)))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-caseNumber").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK and display search page with not found message when user not authorised to view claim" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Right(Some(claimDetailWithOtherEori))))
      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-0005").url)
        val result  = route(app, request).value

        status(result)                                                        shouldBe OK
        contentAsString(result).contains("No matching results for NDRC-0005") shouldBe true
      }
    }

    "redirect to the unverified email page when there is no active email found" in new Setup {
      (mockDataStoreConnector
        .getEmail(_: String)(_: HeaderCarrier))
        .expects(*, *)
        .returning(
          Future.successful(Left(UnverifiedEmail))
        )

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-caseNumber").url)
        val result  = route(app, request).value
        status(result) shouldBe SEE_OTHER
      }
    }

    "return OK when there is email check connectivity issue" in new Setup {
      (mockDataStoreConnector
        .getEmail(_: String)(_: HeaderCarrier))
        .expects(*, *)
        .returning(
          Future.failed(new Exception("email check fails"))
        )
      (mockDataStoreConnector
        .getCompanyName(_: String)(_: HeaderCarrier))
        .stubs(*, *)
        .returning(
          Future.successful(Some("companyName"))
        )
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Right(Some(claimDetail))))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-caseNumber").url)
        val result  = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return OK and display search page with not found message when claim not found from the API" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Right(None)))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-1234").url)
        val result  = route(app, request).value

        status(result)                                                        shouldBe OK
        contentAsString(result).contains("No matching results for NDRC-1234") shouldBe true
      }
    }

    "redirect to error page when API returns 500" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Left("ERROR_HTTP_500")))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-1234").url)
        val result  = route(app, request).value
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          routes.ErrorNewTaxTypeCodeValidationController.showError("NDRC-1234").url
        )
      }
    }

    "return NOT_FOUND when API returns other error" in new Setup {
      stubEmailAndCompanyName
      (mockClaimsConnector
        .getAllClaims(_: Boolean)(_: HeaderCarrier))
        .expects(*, *)
        .returning(Future.successful(allClaims))
      (mockClaimsConnector
        .getClaimInformation(_: String, _: ServiceType, _: Option[String])(_: HeaderCarrier))
        .expects(*, *, *, *)
        .returning(Future.successful(Left("FOO")))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("NDRC-1234").url)
        val result  = route(app, request).value
        status(result) shouldBe NOT_FOUND
      }
    }
  }

  trait Setup extends SetupBase {

    val claimDetail: ClaimDetail = ClaimDetail(
      caseNumber = "NDRC-caseNumber",
      serviceType = NDRC,
      declarationId = Some("DeclarationId"),
      mrn = Seq(ProcedureDetail(MRNNumber = "DeclarationId", mainDeclarationReference = true)),
      entryNumbers = Seq.empty,
      lrn = Some("SomeLrn"),
      claimantsEori = Some("GB746502538945"),
      declarantEori = "GB746502538945",
      importerEori = Some("GB746502538945"),
      claimStatus = InProgress,
      caseSubStatus = None,
      claimType = Some(C285),
      caseType = Some(Bulk),
      claimStartDate = Some(LocalDate.now),
      claimClosedDate = None,
      totalClaimAmount = Some("1200"),
      claimantsName = Some("Sarah Philips"),
      claimantsEmail = Some("sarah.philips@acmecorp.com")
    )

    val claimDetailWithOtherEori: ClaimDetail = ClaimDetail(
      caseNumber = "NDRC-0005",
      serviceType = NDRC,
      declarationId = Some("DeclarationId"),
      mrn = Seq(ProcedureDetail(MRNNumber = "DeclarationId", mainDeclarationReference = true)),
      entryNumbers = Seq.empty,
      lrn = Some("SomeLrn"),
      claimantsEori = Some("GB000000000001"),
      declarantEori = "GB000000000001",
      importerEori = Some("GB000000000001"),
      claimStatus = InProgress,
      caseSubStatus = None,
      claimType = Some(C285),
      caseType = Some(Bulk),
      claimStartDate = Some(LocalDate.now),
      claimClosedDate = None,
      totalClaimAmount = Some("1200"),
      claimantsName = Some("Sarah Philips"),
      claimantsEmail = Some("sarah.philips@acmecorp.com")
    )

    val allClaims: AllClaims = AllClaims(
      pendingClaims = Seq.empty,
      inProgressClaims = Seq(
        InProgressClaim("MRN", "NDRC-caseNumber", NDRC, None, Some(LocalDate.of(2021, 2, 1)))
      ),
      closedClaims = Seq.empty
    )

    val app = applicationWithMongoCache.build()
  }

}
