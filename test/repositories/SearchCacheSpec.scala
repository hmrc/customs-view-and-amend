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

import models.{AllClaims, SearchQuery}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.test.Helpers._
import utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global

class SearchCacheSpec extends SpecBase {

  "get" should {
    "return None if no data found in the db" in new Setup {
      await(for {
        result <- database.get("someId")
        _ <- database.collection.drop().toFuture()
      } yield {
        result mustBe None
      })
    }

    "return a SearchQuery if data found in the db" in new Setup {
      await(for {
        _ <- database.set("someId", AllClaims(Seq.empty, Seq.empty, Seq.empty), "testing")
        result <- database.get("someId")
        _ <- database.collection.drop().toFuture()
      } yield {
        result.value mustBe SearchQuery(AllClaims(Seq.empty, Seq.empty, Seq.empty), "testing")
      })
    }
  }

  "set" should {
    "insert a document correctly" in new Setup {
      await(for {
        _ <- database.set("someId", AllClaims(Seq.empty, Seq.empty, Seq.empty), "testing")
        result <- database.get("someId")
        _ <- database.collection.drop().toFuture()
      } yield {
        result.value mustBe SearchQuery(AllClaims(Seq.empty, Seq.empty, Seq.empty), "testing")
      })
    }
  }

  "removeSearch" should {
    "remove a search query from the database" in new Setup {
      await(for {
        _ <- database.set("someId", AllClaims(Seq.empty, Seq.empty, Seq.empty), "testing")
        result1 <- database.get("someId")
        _ <- database.removeSearch("someId")
        result2 <- database.get("someId")
        _ <- database.collection.drop().toFuture()
      } yield {
        result1.value mustBe SearchQuery(AllClaims(Seq.empty, Seq.empty, Seq.empty), "testing")
        result2 mustBe None
      })
    }
  }


  trait Setup {
    val app: Application = application.build()
    val database: DefaultSearchCache = app.injector.instanceOf[DefaultSearchCache]
  }
}
