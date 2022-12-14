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

import models.responses.ClaimType
import play.api.libs.json._
import play.api.mvc.PathBindable

sealed trait CaseType

case object Single extends CaseType
case object Multiple extends CaseType
case object CMA extends CaseType
case object C18 extends CaseType

object CaseType {
  implicit def pathBindable: PathBindable[CaseType] = new PathBindable[CaseType] {
    override def bind(key: String, value: String): Either[String, CaseType] =
      value match {
        case "Individual" => Right(Single)
        case "Bulk" => Right(Multiple)
        case "CMA" => Right(CMA)
        case "C18" => Right(C18)
        case _ => Left("Invalid caseType")
      }

    override def unbind(key: String, value: CaseType): String = {
      value match {
        case Single => "Individual"
        case Multiple => "Bulk"
        case CMA => "CMA"
        case C18 => "C18"
      }
    }
  }

  implicit val format: Format[CaseType] = new Format[CaseType] {
    override def writes(o: CaseType): JsValue =
      o match {
        case Single => JsString("Individual")
        case Multiple => JsString("Bulk")
        case CMA => JsString("CMA")
        case C18 => JsString("C18")
      }

    override def reads(json: JsValue): JsResult[CaseType] =
      json match {
        case JsString("Individual") => JsSuccess(Single)
        case JsString("Bulk") => JsSuccess(Multiple)
        case JsString("CMA") => JsSuccess(CMA)
        case JsString("C18")  =>   JsSuccess(C18)
        case e => JsError(s"Unexpected caseType from TPI02: $e")
      }
  }
}

