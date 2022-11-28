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

package services

import connector.{ClaimsConnector, UploadDocumentsConnector}
import models.{AllClaims, ClosedClaim, InProgressClaim, NDRC, PendingClaim}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.{ClaimsCache, ClaimsMongo, UploadedFilesCache}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class ClaimServiceSpec extends SpecBase {

  "authorisedToView" should {
    "return the result of getSpecificCase" in new Setup {
      when(mockClaimsConnector.getClaims(any)(any))
        .thenReturn(Future.successful(allClaims))
      when(mockClaimsCache.getSpecificCase(any, any))
        .thenReturn(Future.successful(Some(claimsMongo)))

      running(app) {
        val result = await(service.authorisedToView("caseNumber", "EORI"))
        result.value mustBe claimsMongo
      }
    }
  }

  "clearUploaded" should {
    "Not wipe the data if initial request is 'false'" in new Setup {
      running(app) {
        await(service.clearUploaded("someCase", initialRequest = false))
        verifyZeroInteractions(mockUploadedFilesCache)
        verifyZeroInteractions(mockUploadDocumentsConnector)
      }
    }

    "Wipe the data if initial request is 'true'" in new Setup {
      when(mockUploadedFilesCache.removeRecord(any))
        .thenReturn(Future.successful(true))
      when(mockUploadDocumentsConnector.wipeData())
        .thenReturn(Future.successful(true))
      running(app) {
        await(service.clearUploaded("someCase", initialRequest = true))
      }
    }
  }

  trait Setup {
    val claimsMongo: ClaimsMongo = ClaimsMongo(
      Seq(InProgressClaim("MRN", "caseNumber", NDRC, None, LocalDate.of(2021, 10, 23))),
      LocalDateTime.now()
    )

    val closedClaims: Seq[ClosedClaim]        = (1 to 100).map { value =>
      ClosedClaim(
        "MRN",
        s"NDRC-${1000 + value}",
        NDRC,
        None,
        LocalDate.of(2021, 2, 1).plusDays(value),
        LocalDate.of(2022, 1, 1).plusDays(value),
        "Closed"
      )
    }
    val pendingClaims: Seq[PendingClaim]      = (1 to 100).map { value =>
      PendingClaim(
        "MRN",
        s"NDRC-${2000 + value}",
        NDRC,
        None,
        LocalDate.of(2021, 2, 1).plusDays(value),
        LocalDate.of(2022, 1, 1).plusDays(value)
      )
    }
    val inProgressClaim: Seq[InProgressClaim] = (1 to 100).map { value =>
      InProgressClaim("MRN", s"NDRC-${3000 + value}", NDRC, None, LocalDate.of(2021, 2, 1).plusDays(value))
    }

    val allClaims: AllClaims = AllClaims(
      pendingClaims = pendingClaims,
      inProgressClaims = inProgressClaim,
      closedClaims = closedClaims
    )

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockClaimsConnector: ClaimsConnector                   = mock[ClaimsConnector]
    val mockUploadDocumentsConnector: UploadDocumentsConnector = mock[UploadDocumentsConnector]
    val mockUploadedFilesCache: UploadedFilesCache             = mock[UploadedFilesCache]
    val mockClaimsCache: ClaimsCache                           = mock[ClaimsCache]

    val app: Application = application
      .overrides(
        inject.bind[ClaimsConnector].toInstance(mockClaimsConnector),
        inject.bind[UploadDocumentsConnector].toInstance(mockUploadDocumentsConnector),
        inject.bind[UploadedFilesCache].toInstance(mockUploadedFilesCache),
        inject.bind[ClaimsCache].toInstance(mockClaimsCache)
      )
      .build()

    val service: ClaimService = app.injector.instanceOf[ClaimService]
  }
}
