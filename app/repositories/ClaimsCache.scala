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

import models.Claim
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.{Configuration, Logger}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultClaimsCache @Inject() (mongo: MongoComponent, config: Configuration)(implicit
  executionContext: ExecutionContext
) extends PlayMongoRepository[ClaimsMongo](
      collectionName = "all-claims-cache",
      mongoComponent = mongo,
      domainFormat = ClaimsMongo.format,
      indexes = Seq(
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions()
            .name("all-claims-cache-last-updated-index")
            .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
        )
      )
    )
    with ClaimsCache {

  override def set(id: String, claims: Seq[Claim]): Future[Boolean] =
    collection
      .replaceOne(
        equal("_id", id),
        ClaimsMongo(claims, LocalDateTime.now()),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
      .transform {
        case scala.util.Failure(exception) =>
          // $COVERAGE-OFF$
          Logger(getClass).error(s"An attempt to upsert claims for $id resulted in $exception")
          scala.util.Failure(exception)
        // $COVERAGE-ON$
        case result                        => result
      }

  override def get(id: String): Future[Option[Seq[Claim]]] =
    collection
      .find(equal("_id", id))
      .toSingle()
      .toFutureOption()
      .map(_.map(_.claims))
      .transform {
        case scala.util.Failure(exception) =>
          // $COVERAGE-OFF$
          Logger(getClass).error(s"An attempt to retrieve claims for $id resulted in $exception")
          scala.util.Failure(exception)
        // $COVERAGE-ON$
        case result                        => result
      }

  override def getSpecificCase(id: String, caseNumber: String): Future[Option[ClaimsMongo]] =
    collection
      .find(equal("claims.caseNumber", caseNumber))
      .toSingle()
      .toFutureOption()
      .transform {
        case scala.util.Failure(exception) =>
          // $COVERAGE-OFF$
          Logger(getClass).error(
            s"An attempt to retrieve specific claims for $id and $caseNumber resulted in $exception"
          )
          scala.util.Failure(exception)
        // $COVERAGE-ON$
        case result                        => result
      }
}

sealed trait Failure
case object NoCaseFound extends Failure

trait ClaimsCache {
  def set(id: String, claims: Seq[Claim]): Future[Boolean]
  def get(id: String): Future[Option[Seq[Claim]]]
  def getSpecificCase(id: String, caseNumber: String): Future[Option[ClaimsMongo]]
}

case class ClaimsMongo(claims: Seq[Claim], lastUpdated: LocalDateTime)

object ClaimsMongo {
  implicit val timeFormat: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[ClaimsMongo]      = Json.format[ClaimsMongo]
}
