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
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.SpecBase

import scala.concurrent.Future

class EmailControllerSpec extends SpecBase {

  "showUnverified" should {
    "return OK" in new Setup {
      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.EmailController.showUnverified().url)
      val result                                       = route(app, request).value
      status(result) shouldBe OK
    }
  }

  trait Setup extends SetupBase {
    val app = application.build()
  }

}
