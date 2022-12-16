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

package repositories

import models.{InProgressClaim, NDRC}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.test.Helpers._
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class ClaimsCacheSpec extends SpecBase {

  "set and get" should {
    "populate data into the mongo" in new Setup {
      await(for {
        _      <- database.set("someId", claims)
        result <- database.get("someId")
        _      <- database.collection.drop().toFuture()
      } yield result.value mustBe claims)
    }
  }

  "get" should {
    "return data if populated in the mongo" in new Setup {
      await(for {
        _      <- database.set("someId", claims)
        result <- database.get("someId")
        _      <- database.collection.drop().toFuture()
      } yield result.value mustBe claims)
    }

    "return None if no data populated in the mongo" in new Setup {
      await(for {
        _      <- database.set("someId", claims)
        result <- database.get("empty")
        _      <- database.collection.drop().toFuture()
      } yield result mustBe None)
    }
  }

  "hasCaseNumber" should {
    "return true if case number present in database" in new Setup {
      await(for {
        _      <- database.set("someId", claims)
        result <- database.getSpecificCase("someId", "NDRC-2022")
        _      <- database.collection.drop().toFuture()
      } yield result.nonEmpty mustBe true)
    }

    "return false if case number present in database" in new Setup {
      await(for {
        _      <- database.set("someId", claims)
        result <- database.getSpecificCase("someId", "INVALID")
        _      <- database.collection.drop().toFuture()
      } yield result.nonEmpty mustBe false)
    }
  }

  trait Setup extends SetupBase {
    val app: Application             = application.build()
    val claims                       = Seq(InProgressClaim("MRN", "NDRC-2022", NDRC, None, LocalDate.of(2019, 1, 1)))
    val database: DefaultClaimsCache = app.injector.instanceOf[DefaultClaimsCache]
  }
}
