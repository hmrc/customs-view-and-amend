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

package models

import play.api.mvc.Headers
import utils.Hash

import java.util.Locale
import java.util.UUID
import scala.util.matching.Regex

object CorrelationIdHeader {

  val headerName                  = "X-Correlation-ID"
  val headerNameLowercase: String = headerName.toLowerCase(Locale.UK)

  def random(): (String, String) = (headerName, UUID.randomUUID().toString)

  def apply(headerValue: String): (String, String) = (headerName, headerValue)

  def from(eori: String): (String, String) =
    (headerName, Hash(eori).take(8) + UUID.randomUUID().toString.drop(8))

  def from(uuid: UUID): (String, String) = (headerName, uuid.toString)

  def from(eori: String, sessionId: Option[String]): (String, String) = {
    val uuid = UUID.randomUUID().toString
    (
      headerName,
      Hash(eori).take(8) +
        sessionId
          .flatMap(getUuidPart(_))
          .map(_ + uuid.drop(18))
          .getOrElse(uuid.drop(8))
    )
  }

  def from(eori: String, uuid: UUID): (String, String) =
    (headerName, Hash(eori).take(8) + uuid.toString.drop(8).take(10) + UUID.randomUUID().toString.drop(18))

  val uuidRegex: Regex = "^.*\\w{8}(-\\w{4}-\\w{4})-\\w{4}-\\w{12}.*$".r

  def getUuidPart(value: String): Option[String] =
    value match {
      case uuidRegex(a) => Some(a)
      case _            => None
    }

  implicit class HeaderOps(val headers: Headers) extends AnyVal {
    def addIfMissing(newHeader: (String, String)): Headers =
      if (
        headers.keys
          .map(_.toLowerCase(Locale.UK))
          .contains(newHeader._1)
      ) headers
      else
        headers.add(newHeader)
  }

}
