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
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Configuration
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultSearchCache @Inject()(mongo: MongoComponent, config: Configuration)(implicit executionContext: ExecutionContext)
  extends PlayMongoRepository[SearchQueryMongo](
    collectionName = "search-cache",
    mongoComponent = mongo,
    domainFormat = SearchQueryMongo.format,
    indexes = Seq(
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions().name("search-cache-last-updated-index")
          .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
      )
    )) with SearchCache {

  override def get(id: String): Future[Option[SearchQuery]] =
    collection.find(equal("_id", id))
      .toSingle()
      .toFutureOption()
      .map(_.map(_.toSearchQuery))


  override def set(id: String, claims: AllClaims, query: String): Future[Boolean] =
    collection.replaceOne(
      equal("_id", id),
      SearchQueryMongo(claims, query, LocalDateTime.now()),
      ReplaceOptions().upsert(true)
    ).toFuture().map(_.wasAcknowledged())

  override def removeSearch(id: String): Future[Boolean] =
    collection.deleteOne(equal("_id", id))
      .toFuture()
      .map(_.wasAcknowledged())
}

trait SearchCache {
  def get(id: String): Future[Option[SearchQuery]]
  def set(id: String, claims: AllClaims, query: String): Future[Boolean]
  def removeSearch(id: String): Future[Boolean]
}

case class SearchQueryMongo(claims: AllClaims, query: String, lastUpdated: LocalDateTime) {
  def toSearchQuery: SearchQuery = SearchQuery(claims, query)
}

object SearchQueryMongo {
  implicit val timeFormat: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[SearchQueryMongo] = Json.format[SearchQueryMongo]
}