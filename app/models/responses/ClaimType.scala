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

package models.responses

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

sealed trait ClaimType

case object C285 extends ClaimType

case object `C&E1179` extends ClaimType

object ClaimType {
  implicit def pathBindable: PathBindable[ClaimType] = new PathBindable[ClaimType] {
    override def bind(key: String, value: String): Either[String, ClaimType] =
      value match {
        case "C285" => Right(C285)
        case "CE1179" => Right(`C&E1179`)
        case _ => Left("Invalid service type")
      }

    override def unbind(key: String, value: ClaimType): String = {
      value match {
        case `C&E1179` => "CE1179"
        case C285 => "C285"
      }
    }
  }

  implicit def queryBindable: QueryStringBindable[ClaimType] = new QueryStringBindable[ClaimType] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ClaimType]] =
      params(key) match {
        case "C285" :: Nil => Some(Right(C285))
        case "CE1179" :: Nil => Some(Right(`C&E1179`))
        case _ => Some(Left("Invalid service type"))
      }

    override def unbind(key: String, value: ClaimType): String = {
      value match {
        case `C&E1179` => s"$key=CE1179"
        case C285 => s"$key=C285"
      }
    }
  }

  implicit val format: Format[ClaimType] = new Format[ClaimType] {
    override def writes(o: ClaimType): JsValue =
      o match {
        case C285 => JsString("C285")
        case `C&E1179` => JsString("C&E1179")
      }

    override def reads(json: JsValue): JsResult[ClaimType] =
      json match {
        case JsString("C285") => JsSuccess(C285)
        case JsString("C&E1179") => JsSuccess(`C&E1179`)
        case e => JsError(s"Unexpected claimType from TPI02: $e")
      }
  }
}




