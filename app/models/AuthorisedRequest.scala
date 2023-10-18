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

import play.api.mvc.{Request, WrappedRequest}
import play.api.mvc.Headers
import models.CorrelationIdHeader
import models.CorrelationIdHeader._
import uk.gov.hmrc.http.HeaderNames

final case class AuthorisedRequest[A](
  request: Request[A],
  eori: String,
  isCallback: Boolean = false
) extends WrappedRequest[A](request)
    with RequestWithEori[A] {

  override def headers: Headers =
    request.headers
      .addIfMissing(CorrelationIdHeader.from(eori, request.headers.get(HeaderNames.xSessionId)))

  def withSessionData(sessionData: SessionData): AuthorisedRequestWithSessionData[A] =
    AuthorisedRequestWithSessionData(request, eori, sessionData)
}
