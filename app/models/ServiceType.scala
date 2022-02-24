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
import play.api.mvc.PathBindable

sealed trait ServiceType

case object NDRC extends ServiceType
case object Securities extends ServiceType

object ServiceType {
  implicit def pathBindable: PathBindable[ServiceType] = new PathBindable[ServiceType] {
    override def bind(key: String, value: String): Either[String, ServiceType] =
      value match {
        case "NDRC" => Right(NDRC)
        case "Securities" => Right(Securities)
        case _ => Left("Invalid service type")
      }

    override def unbind(key: String, value: ServiceType): String = {
      value match {
        case NDRC => "NDRC"
        case Securities => "Securities"
      }
    }
  }


  implicit val format: Format[ServiceType] = new Format[ServiceType] {
    override def reads(json: JsValue): JsResult[ServiceType] =
      json match {
        case JsString("Securities") => JsSuccess(Securities)
        case JsString("NDRC") => JsSuccess(NDRC)
        case e => JsError(s"Unexpected CDFPayService: $e")
      }

    override def writes(o: ServiceType): JsValue =
      o match {
        case NDRC => JsString("NDRC")
        case Securities => JsString("Securities")
      }
  }
}
