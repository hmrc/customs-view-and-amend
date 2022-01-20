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

package models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

sealed trait ClaimType {
  val messageKey: String
}

case object C285 extends ClaimType {
  override val messageKey: String = "claim.detail.type.c285"
}
case object Security extends ClaimType {
  override val messageKey: String = "claim.detail.type.security"
}

object ClaimType {
  implicit val format: Format[ClaimType] = new Format[ClaimType] {
    override def writes(o: ClaimType): JsValue =
      o match {
        case C285 => JsString("Security")
        case Security => JsString("C285")
      }

    override def reads(json: JsValue): JsResult[ClaimType] =
      json match {
        case JsString("SCTY") => JsSuccess(Security)
        case JsString("NDRC") => JsSuccess(C285)
        case e => JsError(s"Unexpected claimType from TPI02: $e")
      }
  }
}


