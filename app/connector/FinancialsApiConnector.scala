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

package connector

import models.{Claim, ClaimDetail, ClosedClaim, InProgress, InProgressClaim, PendingClaim}
import repositories.ClaimsCache

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialsApiConnector @Inject()(claimsCache: ClaimsCache)(implicit executionContext: ExecutionContext) {

  def getClaims(eori: String): Future[Seq[Claim]] = {
    claimsCache.get(eori).flatMap {
      case Some(value) => Future.successful(value)
      case None =>
        simulateRetrieval().flatMap { claims =>
          claimsCache.set(eori, claims).map { _ => claims }
        }
    }
  }

  def getClaimInformation(caseNumber: String) = Future.successful(
    ClaimDetail(
      caseNumber,
      Seq("21GB03I52858073821"),
      "GB746502538945",
      "InProgress",
      LocalDate.of(2021, 10, 23),
      1200,
      "Sarah Philips",
      "sarah.philips@acmecorp.com"
    ))

  def simulateRetrieval(): Future[Seq[Claim]] =
    Future.successful(Seq(
      ClosedClaim("NDRC-6666", LocalDate.of(2021, 2, 1), LocalDate.of(2022, 1, 1)),
      ClosedClaim("NDRC-4592", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 5, 1)),
      ClosedClaim("NDRC-8318", LocalDate.of(2021, 4, 1), LocalDate.of(2022, 2, 1)),
      ClosedClaim("NDRC-2318", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 9, 1)),
      ClosedClaim("NDRC-8496", LocalDate.of(2021, 6, 1), LocalDate.of(2022, 7, 1)),
      PendingClaim("NDRC-1965", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 18)),
      PendingClaim("NDRC-7321", LocalDate.of(2021, 3, 1), LocalDate.of(2021, 2, 18)),
      PendingClaim("NDRC-3398", LocalDate.of(2021, 4, 1), LocalDate.of(2021, 2, 18)),
      PendingClaim("NDRC-7792", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 18)),
      PendingClaim("NDRC-3216", LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 18)),
      PendingClaim("NDRC-7876", LocalDate.of(2021, 3, 1), LocalDate.of(2021, 2, 18)),
      PendingClaim("NDRC-1294", LocalDate.of(2021, 7, 1), LocalDate.of(2021, 2, 18)),
      InProgressClaim("NDRC-7935", LocalDate.of(2021, 1, 1), newMessage = true),
      InProgressClaim("NDRC-8975", LocalDate.of(2021, 3, 1), newMessage = true),
      InProgressClaim("NDRC-3789", LocalDate.of(2021, 7, 1), newMessage = true),
      InProgressClaim("NDRC-3753", LocalDate.of(2021, 8, 1), newMessage = true),
      InProgressClaim("NDRC-4567", LocalDate.of(2021, 1, 1), newMessage = true),
      InProgressClaim("NDRC-0476", LocalDate.of(2021, 6, 1), newMessage = true),
      InProgressClaim("NDRC-8874", LocalDate.of(2021, 8, 1), newMessage = true)
    ))

}
