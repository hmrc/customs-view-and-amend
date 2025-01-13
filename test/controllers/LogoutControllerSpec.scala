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

import play.api.Application
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase

class LogoutControllerSpec extends SpecBase {

  "LogoutController logout" should {
    "redirect to sign out page" in new Setup {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.LogoutController.logout.url).withHeaders("X-Session-Id" -> "someSession")
      running(app) {
        val result = route(app, request).value
        redirectLocation(
          result
        ).value shouldBe "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http%3A%2F%2Flocalhost%3A7500%2Fclaim-back-import-duty-vat%2Fsign-out"
      }
    }
  }

  "LogoutController logout no survey" should {
    "redirect to sign-out" in new Setup {
      running(app) {
        val request =
          fakeRequest(GET, routes.LogoutController.logoutNoSurvey.url).withHeaders("X-Session-Id" -> "someSession")
        val result  = route(app, request).value
        redirectLocation(
          result
        ).value shouldBe "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http%3A%2F%2Flocalhost%3A7500%2Fclaim-back-import-duty-vat%2Fsign-out"
      }
    }
  }

  trait Setup extends SetupBase {
    val app: Application = application
      .build()
  }
}
