/*
 * Copyright 2023 HM Revenue & Customs
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
import models.file_upload.UploadedFile
import models.{AllClaims, ClosedClaim, FileSelection, InProgressClaim, NDRC, PendingClaim, SessionData, XiEori}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SessionCacheSpec extends AnyWordSpec with CleanMongoCollectionSupport with Matchers with Eventually {

  import SessionCacheSpec._

  val sessionCache =
    new DefaultSessionCache(mongoComponent, new CurrentTimestampSupport(), config)

  "SessionCache" should {

    "be able to insert empty SessionData into mongo and read it back" in new TestEnvironment {
      val sessionData: SessionData = SessionData()
      val result                   = sessionCache.store(sessionData)

      await(result) should be(Right(()))

      eventually {
        val getResult = sessionCache.get()
        await(getResult) should be(Right(Some(sessionData)))
      }
    }

    "be able to insert full SessionData into mongo and read it back" in new TestEnvironment {
      val sessionData: SessionData = SessionData()
        .withCompanyName("Foo Ltd.")
        .withUploadedFiles(
          Seq(
            UploadedFile(
              "ref",
              "/uri",
              "timestamp",
              "sum",
              "file",
              "mime",
              10,
              None,
              FileSelection.AdditionalSupportingDocuments,
              None
            )
          )
        )
        .withDocumentType(FileSelection.CalculationWorksheet)
        .withInitialFileUploadData("ABC123")
        .withVerifiedEmail("foo@bar.com")
        .withXiEori(None)
        .withAllClaims(
          AllClaims(
            pendingClaims = Seq(
              PendingClaim(
                "MRN",
                "NDRC-0001",
                NDRC,
                None,
                Some(LocalDate.of(2019, 1, 1)),
                Some(LocalDate.of(2019, 2, 1))
              )
            ),
            inProgressClaims = Seq(InProgressClaim("MRN", "NDRC-0002", NDRC, None, Some(LocalDate.of(2019, 1, 1)))),
            closedClaims = Seq(
              ClosedClaim(
                "MRN",
                "NDRC-0003",
                NDRC,
                None,
                Some(LocalDate.of(2019, 1, 1)),
                Some(LocalDate.of(2019, 2, 1)),
                "Closed"
              )
            )
          )
        )
        .withSubmitted

      val result = sessionCache.store(sessionData)

      await(result) should be(Right(()))

      eventually {
        val getResult = sessionCache.get()
        await(getResult) should be(Right(Some(sessionData)))
      }
    }

    "be able to insert half-full SessionData into mongo and read it back" in new TestEnvironment {
      val sessionData: SessionData = SessionData()
        .withCompanyName("Foo Ltd.")
        .withXiEori(Some(XiEori("abc", "def")))

      val result = sessionCache.store(sessionData)

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

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        | session-store.expiry-time = 7 days
        |""".stripMargin
    )
  )

  class TestEnvironment {
    val sessionId: SessionId       = SessionId(UUID.randomUUID().toString)
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(sessionId))
  }

}
