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

import play.api.libs.json._

sealed trait ClaimStatus {
  val messageKey: String
}

case object InProgress extends ClaimStatus {
  val messageKey = "claim.inProgress"
}
// Add github ssh
case object Pending extends ClaimStatus {
  val messageKey = "claim.pending"
}
case object Closed extends ClaimStatus {
  val messageKey = "claim.closed"
}

object ClaimStatus {
  implicit val format: Format[ClaimStatus] = new Format[ClaimStatus] {
    override def reads(json: JsValue): JsResult[ClaimStatus] =
      json match {
        case JsString("InProgress") => JsSuccess(InProgress)
        case JsString("Pending") => JsSuccess(Pending)
        case JsString("Closed") => JsSuccess(Closed)
        case e => JsError(s"Unable to parse claim type: $e")
      }


    override def writes(o: ClaimStatus): JsValue =
      o match {
        case InProgress => JsString("InProgress")
        case Pending => JsString("Pending")
        case Closed => JsString("Closed")
      }
  }
}
