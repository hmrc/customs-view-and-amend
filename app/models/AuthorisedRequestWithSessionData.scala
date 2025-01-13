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

import models.CorrelationIdHeader.*
import play.api.mvc.{Headers, Request, WrappedRequest}
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}

final case class AuthorisedRequestWithSessionData[A](
  request: Request[A],
  eori: String,
  sessionData: SessionData
) extends WrappedRequest[A](request)
    with RequestWithEori[A]
    with RequestWithSessionData[A] {

  override def headers: Headers =
    request.headers
      .addIfMissing(
        CorrelationIdHeader.from(
          eori,
          request.session
            .get(SessionKeys.sessionId)
            .orElse(request.headers.get(HeaderNames.xSessionId))
        )
      )

  def withAllClaims(allClaims: AllClaims): AuthorisedRequestWithSessionData[A] =
    copy(sessionData = sessionData.withAllClaims(allClaims))

  def withXiEori(xiEori: Option[XiEori]): AuthorisedRequestWithSessionData[A] =
    copy(sessionData = sessionData.withXiEori(xiEori))
}
