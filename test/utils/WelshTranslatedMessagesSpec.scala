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

import org.scalatest.Ignore
import play.api.Application
import play.api.i18n.MessagesApi

@Ignore //remove this line to run test or once we have translated all the messages
class WelshTranslatedMessagesSpec extends SpecBase {

  "WelshTranslatedMessagesSpec" when {
    "ensure all english messages" should {
      "have a welsh translation" in new Setup {

        lazy val serviceMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

        val serviceMessages  = serviceMessagesApi.messages
        val englishMessages  = serviceMessages("default")
        val welshMessages    = serviceMessages("cy")
        val missingWelshKeys = englishMessages.keySet.filterNot(welshMessages.keySet)

        if (missingWelshKeys.nonEmpty) {
          val failureText =
            missingWelshKeys.foldLeft(s"There are ${missingWelshKeys.size} missing Welsh translations:") {
              case (failureString, key) =>
                failureString + s"\n$key=${englishMessages(key)}"
            }

          fail(failureText)
        }
      }
    }
  }

  trait Setup extends SetupBase {
    val app: Application = application.build()
  }

}
