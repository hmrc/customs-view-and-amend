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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase

import scala.concurrent.Future

class EmailControllerSpec extends SpecBase {

  "showUnverified" should {
    "return OK" in new Setup {
      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.EmailController.showUnverified().url)
      val result: Future[Result]                       = route(app, request).value
      status(result) mustBe OK
    }
  }

  trait Setup extends SetupBase {
    val app: Application = application.build()
  }

}
