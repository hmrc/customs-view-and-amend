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

import cats.syntax.eq._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.{Error, SessionData}
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[DefaultSessionCache])
trait SessionCache {

  def get()(implicit
    hc: HeaderCarrier
  ): Future[Either[Error, Option[SessionData]]]

  def store(sessionData: SessionData)(implicit
    hc: HeaderCarrier
  ): Future[Either[Error, Unit]]

  def update(modify: SessionData => SessionData)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Error, SessionData]]

}

object HeaderCarrierCacheId extends CacheIdType[HeaderCarrier] {

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  override def run: HeaderCarrier => String =
    _.sessionId
      .map(_.value)
      .getOrElse(throw NoSessionException)

  case object NoSessionException extends Exception("Could not find sessionId")
}

@Singleton
class DefaultSessionCache @Inject() (
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport,
  configuration: Configuration
)(implicit
  ec: ExecutionContext
) extends MongoCacheRepository[HeaderCarrier](
      mongoComponent = mongoComponent,
      collectionName = "sessions",
      ttl = configuration.get[FiniteDuration]("session-store.expiry-time"),
      timestampSupport = timestampSupport,
      cacheIdType = HeaderCarrierCacheId
    )
    with SessionCache {

  private val sessionDataKey: DataKey[SessionData] =
    DataKey[SessionData]("customs-view-and-amend-session")

  final def get()(implicit
    hc: HeaderCarrier
  ): Future[Either[Error, Option[SessionData]]] =
    try
      get[SessionData](hc)(sessionDataKey)
        .map(Right(_))
        .recover { case NonFatal(e) => Left(Error(e)) }
    catch {
      case NonFatal(e) => Future.successful(Left(Error(e)))
    }

  final def store(
    sessionData: SessionData
  )(implicit hc: HeaderCarrier): Future[Either[Error, Unit]] =
    try
      put(hc)(sessionDataKey, sessionData)
        .map(_ => Right(()))
        .recover { case NonFatal(e) => Left(Error(e)) }
    catch {
      case NonFatal(e) => Future.successful(Left(Error(e)))
    }

  final def update(modify: SessionData => SessionData)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Error, SessionData]] =
    get().flatMap {
      case Right(Some(existingSessionData)) =>
        try {
          val modifiedSessionData = modify(existingSessionData)
          if (modifiedSessionData =!= existingSessionData)
            store(modifiedSessionData)
              .map(_.map(_ => modifiedSessionData))
          else
            Future.successful(Right(existingSessionData))
        } catch {
          case NonFatal(e) =>
            Future.successful(Left(Error(e)))
        }

      case Right(None) =>
        Future.successful(
          Left(Error("no session found in mongo"))
        )

      case Left(error) =>
        Future.successful(Left(error))
    }

}
