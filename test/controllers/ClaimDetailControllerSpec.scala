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

package controllers

import connector.ClaimsConnector
import models._
import models.email.UnverifiedEmail
import models.responses.{C285, ProcedureDetail}
import org.scalatest.matchers.must.Matchers._
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.{ClaimsCache, ClaimsMongo}
import services.ClaimService
import uk.gov.hmrc.auth.core.retrieve.Email
import utils.SpecBase

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class ClaimDetailControllerSpec extends SpecBase {

  "claimDetail" should {
    "return OK when a in progress claim has been found" in new Setup {
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail)))
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", SCTY, searched = false).url)
        val result  = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK when a pending claim has been found" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail.copy(claimStatus = Pending))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", NDRC, searched = false).url)
        val result  = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return OK when a closed claim has been found" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(Some(claimDetail.copy(claimStatus = Closed))))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", NDRC, searched = false).url)
        val result  = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return NOT_FOUND when user not authorised to view claim" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", SCTY, searched = false).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND when there is no active email found" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockDataStoreConnector.getEmail(any)(any))
        .thenReturn(Future.successful(Left(UnverifiedEmail)))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", NDRC, searched = true).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND when claim not found from the API" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", NDRC, searched = true).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return NOT_FOUND when a claim is not present in the list of claims" in new Setup {
      when(mockClaimService.authorisedToView(any, any)(any))
        .thenReturn(Future.successful(Some(claimsMongo)))
      when(mockClaimsConnector.getClaimInformation(any, any, any)(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request =
          fakeRequest(GET, routes.ClaimDetailController.claimDetail("someClaim", NDRC, searched = false).url)
        val result  = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }
  }

  trait Setup {
    val mockClaimsCache: ClaimsCache         = mock[ClaimsCache]
    val mockClaimsConnector: ClaimsConnector = mock[ClaimsConnector]
    val claimsMongo: ClaimsMongo             = ClaimsMongo(
      Seq(InProgressClaim("MRN", "someClaim", NDRC, Some("LRN"), LocalDate.of(2021, 10, 23))),
      LocalDateTime.now()
    )
    val mockClaimService: ClaimService       = mock[ClaimService]

    val claimDetail: ClaimDetail = ClaimDetail(
      "caseNumber",
      NDRC,
      "DeclarationId",
      Seq(ProcedureDetail("DeclarationId", true)),
      Seq.empty,
      Some("SomeLrn"),
      Some("GB746502538945"),
      InProgress,
      None,
      Some(C285),
      LocalDate.now,
      None,
      Some("1200"),
      Some("Sarah Philips"),
      Some("sarah.philips@acmecorp.com")
    )

    when(mockDataStoreConnector.getEmail(any)(any))
      .thenReturn(Future.successful(Right(Email("some@email.com"))))

    val app: Application = application
      .overrides(
        inject.bind[ClaimsCache].toInstance(mockClaimsCache),
        inject.bind[ClaimsConnector].toInstance(mockClaimsConnector),
        inject.bind[ClaimService].toInstance(mockClaimService)
      )
      .build()
  }

}
