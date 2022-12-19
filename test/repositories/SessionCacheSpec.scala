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

import com.typesafe.config.ConfigFactory
import models.SessionData
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.json.Writes
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.Reads

class SessionCacheSpec extends AnyWordSpec with CleanMongoCollectionSupport with Matchers with Eventually {

  import SessionCacheSpec._

  val sessionCache =
    new DefaultSessionCache(mongoComponent, new CurrentTimestampSupport(), config)

  "SessionCache" must {

    "be able to insert SessionData into mongo and read it back" in new TestEnvironment {
      val sessionData: SessionData = SessionData()
      val result                   = sessionCache.store(sessionData)

      await(result) should be(Right(()))

      eventually {
        val getResult = sessionCache.get()
        await(getResult) should be(Right(Some(sessionData)))
      }
    }

    "be able to update SessionData" in new TestEnvironment {
      val sessionData: SessionData = SessionData()

      await(sessionCache.store(sessionData)) should be(Right(()))

      val result = sessionCache.update(_ => SessionData())

      await(result) should be(Right(SessionData()))

      eventually {
        val getResult = sessionCache.get()
        await(getResult) should be(Right(Some(SessionData())))
      }
    }

    "return no SessionData if there is no data in mongo" in new TestEnvironment {
      await(sessionCache.get()) should be(Right(None))
    }

    "return an error there is no session id in the header carrier" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val sessionData: SessionData   = SessionData()

      await(sessionCache.store(sessionData)).isLeft shouldBe true
      await(sessionCache.get()).isLeft              shouldBe true
    }

    "handle store failure" in new TestEnvironment {
      val sessionData: SessionData = SessionData()

      val failingSessionCache =
        new DefaultSessionCache(mongoComponent, new CurrentTimestampSupport(), config) {
          override def put[A : Writes](cacheId: HeaderCarrier)(dataKey: DataKey[A], data: A): Future[CacheItem] =
            Future.failed(new Exception("do not panick"))
        }

      val result = failingSessionCache.store(sessionData)

      await(result).isLeft shouldBe true

      eventually {
        val getResult = failingSessionCache.get()
        await(getResult) shouldBe Right(None)
      }
    }

    "handle get failure" in new TestEnvironment {
      val sessionData: SessionData = SessionData()

      val failingSessionCache =
        new DefaultSessionCache(mongoComponent, new CurrentTimestampSupport(), config) {
          override def get[A : Reads](cacheId: HeaderCarrier)(dataKey: DataKey[A]): Future[Option[A]] =
            Future.failed(new Exception("do not panick"))
        }

      val result = failingSessionCache.store(sessionData)

      await(result) should be(Right(()))

      eventually {
        val getResult = failingSessionCache.get()
        await(getResult).isLeft shouldBe true
      }
    }

    "handle update failure" in new TestEnvironment {
      val sessionData: SessionData = SessionData()

      await(sessionCache.store(sessionData)) should be(Right(()))

      val result = await(sessionCache.update(_ => throw new Exception("do not panick")))
      result.isLeft shouldBe true

      eventually {
        val getResult = sessionCache.get()
        await(getResult) should be(Right(Some(sessionData)))
      }
    }

  }

}

object SessionCacheSpec {

  val config = Configuration(
    ConfigFactory.parseString(
      """
        | session-store.expiry-time = 7 days
        |""".stripMargin
    )
  )

  class TestEnvironment {
    val sessionId                  = SessionId(UUID.randomUUID().toString)
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(sessionId))
  }

}
