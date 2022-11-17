package utils

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

class SimpleStringFormatSpec extends AnyWordSpec with Matchers {

  case class A(i: Int)
  val format = SimpleStringFormat[A](s => A(s.drop(1).toInt), a => s"A${a.i}")

  "SimpleStringFormats" should {
    "serialize an entity as string" in {
      format.writes(A(2))   shouldBe JsString("A2")
      format.writes(A(0))   shouldBe JsString("A0")
      format.writes(A(101)) shouldBe JsString("A101")
    }

    "de-serialize a string as an entity" in {
      format.reads(JsString("A2"))   shouldBe JsSuccess(A(2))
      format.reads(JsString("A0"))   shouldBe JsSuccess(A(0))
      format.reads(JsString("A101")) shouldBe JsSuccess(A(101))
      format.reads(JsNull)           shouldBe a[JsError]
      format.reads(Json.obj())       shouldBe a[JsError]
      format.reads(JsNumber(2))      shouldBe a[JsError]
      format.reads(JsBoolean(true))  shouldBe a[JsError]
    }

  }
}
