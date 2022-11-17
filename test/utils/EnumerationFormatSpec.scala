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

package utils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

class EnumerationFormatSpec extends AnyWordSpec with Matchers {

  import EnumerationFormatSpec._

  "EnumerationFormatSpec" should {
    "serialize an enum" in {
      Foo.format.writes(Foo.A) shouldBe JsString("A")
      Foo.format.writes(Foo.B) shouldBe JsString("B")
      an[Exception]            shouldBe thrownBy {
        Foo.format.writes(Foo.C)
      }
    }

    "de-serialize an enum" in {
      Foo.format.reads(JsString("A"))   shouldBe JsSuccess(Foo.A)
      Foo.format.reads(JsString("B"))   shouldBe JsSuccess(Foo.B)
      an[Exception]                     shouldBe thrownBy {
        Foo.format.reads(JsString("C")) shouldBe a[JsError]
      }
      an[Exception]                     shouldBe thrownBy {
        Foo.format.reads(JsString("D")) shouldBe a[JsError]
      }
      Foo.format.reads(JsNull)          shouldBe a[JsError]
      Foo.format.reads(Json.obj("A" -> JsBoolean(true))) shouldBe a[JsError]
      Foo.format.reads(Json.obj("value" -> JsString("A"))) shouldBe a[JsError]
      Foo.format.reads(JsNumber(1))     shouldBe a[JsError]
      Foo.format.reads(JsBoolean(true)) shouldBe a[JsError]
    }
  }

}

object EnumerationFormatSpec {

  sealed trait Foo
  object Foo extends EnumerationFormat[Foo] {
    case object A extends Foo
    case object B extends Foo
    case object C extends Foo // not included in the value list

    override val values: Set[Foo] = Set(A, B)
  }

}