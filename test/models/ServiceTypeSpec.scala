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

import play.api.libs.json.{JsError, JsResultException, JsString, JsSuccess, Json}
import utils.SpecBase

class ServiceTypeSpec extends SpecBase {

  "ServiceType" should {
    "perform correct path bindings" in {
      ServiceType.pathBindable.bind("someKey", "NDRC") shouldBe Right(NDRC)
      ServiceType.pathBindable.bind("someKey", "SCTY") shouldBe Right(SCTY)
      ServiceType.pathBindable.bind("someKey", "INVALID") shouldBe Left("Invalid service type")
    }

    "perform correct query bindings" in {
      ServiceType.queryBindable.bind("serviceType", Map(("serviceType", Seq("NDRC")))) shouldBe Some(Right(NDRC))
      ServiceType.queryBindable.bind("serviceType", Map(("serviceType", Seq("SCTY")))) shouldBe Some(Right(SCTY))
      ServiceType.queryBindable.bind("serviceType", Map(("serviceType", Seq("Unknown")))) shouldBe Some(Left("Invalid service type"))
      ServiceType.queryBindable.unbind("serviceType", NDRC) shouldBe "serviceType=NDRC"
      ServiceType.queryBindable.unbind("serviceType", SCTY) shouldBe "serviceType=SCTY"
    }

    "write to the correct string" in {
      val ndrcServiceType: ServiceType = NDRC
      val sctyServiceType: ServiceType = SCTY
      Json.toJson(ndrcServiceType) shouldBe JsString("NDRC")
      Json.toJson(sctyServiceType) shouldBe JsString("SCTY")
    }

    "read the correct string" in {
      JsString("SCTY").as[ServiceType] shouldBe SCTY
      JsString("NDRC").as[ServiceType] shouldBe NDRC

      intercept[JsResultException] {
        JsString("INVALID").as[ServiceType]
      }.errors.map(_._2.map(_.message)).head shouldBe List("""Unexpected CDFPayService: "INVALID"""")
    }


  }

}
