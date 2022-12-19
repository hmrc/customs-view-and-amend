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

import models.file_upload.{UploadCargo, UploadedFileMetadata}
import models.{Nonce, _}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.SessionCache
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, SessionId}
import utils.SpecBase

import java.time.LocalDate
import java.util.UUID

class FileUploadControllerSpec extends SpecBase {

  "receiveUpscanCallback" should {
    "return NO_CONTENT when valid callback" in new Setup {
      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(sessionData.fileUploadJourney.get.nonce, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) mustBe NO_CONTENT
      }
    }

    "return UNAUTHORIZED when session not found" in new Setup {
      running(app) {
        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(Nonce.random, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return UNAUTHORIZED when file upload journey not found in session" in new Setup {
      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(Nonce.random, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return UNAUTHORIZED when different nonce then in session" in new Setup {
      running(app) {
        val sessionData = SessionData(Some(allClaimsWithPending))
          .withInitialFileUploadData("claim-123")
        await(sessionCache.store(sessionData)) shouldBe Right(())

        val request = fakeRequest(POST, routes.FileUploadController.receiveUpscanCallback.url)
          .withJsonBody(
            Json.toJson(
              UploadedFileMetadata(Nonce.random, Seq.empty, Some(UploadCargo("NDRC-1000")))
            )
          )
        val result  = route(app, request).value
        status(result) mustBe UNAUTHORIZED
      }
    }

  }

  trait Setup extends SetupBase {
    val app: Application = applicationWithMongoCache.build()

    def sessionCache = app.injector.instanceOf[SessionCache]

    implicit val hc: HeaderCarrier =
      HeaderCarrier(
        authorization = Some(Authorization(UUID.randomUUID().toString)),
        sessionId = Some(SessionId(UUID.randomUUID().toString))
      )

    val allClaimsWithPending: AllClaims = AllClaims(
      pendingClaims = Seq(
        PendingClaim(
          "MRN",
          "claim-123",
          NDRC,
          None,
          LocalDate.of(2021, 2, 1),
          LocalDate.of(2022, 1, 1)
        )
      ),
      inProgressClaims = Seq.empty,
      closedClaims = Seq.empty
    )
  }
}
