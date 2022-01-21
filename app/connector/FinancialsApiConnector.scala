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

import models._
import repositories.ClaimsCache

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialsApiConnector @Inject()(claimsCache: ClaimsCache)(implicit executionContext: ExecutionContext) {

  //TODO handle error cases from API (maybe use eithers)
  def getClaims(eori: String): Future[AllClaims] = {
    claimsCache.get(eori).flatMap {
      case Some(value) => Future.successful(value)
      case None =>
        simulateRetrieval().flatMap { claims =>
          claimsCache.set(eori, claims).map { _ => claims }
        }
    }.map { claims =>
      AllClaims(
        claims.collect { case e: PendingClaim => e },
        claims.collect { case e: InProgressClaim => e },
        claims.collect { case e: ClosedClaim => e }
      )
    }
  }

  //TODO handle error cases from API (maybe use eithers)
  def getClaimInformation(caseNumber: String): Future[ClaimDetail] = Future.successful(
    ClaimDetail(
      caseNumber,
      Seq("21GB03I52858073821"),
      "GB746502538945",
      InProgress,
      C285,
      LocalDate.of(2021, 10, 23),
      1200,
      "Sarah Philips",
      "sarah.philips@acmecorp.com"
    ))

  def simulateRetrieval(): Future[Seq[Claim]] = {
    val closedClaims: Seq[ClosedClaim] = (1 to 2).map { value =>
      ClosedClaim(s"NDRC-${1000 + value}", LocalDate.of(2021, 2, 1).plusDays(value), LocalDate.of(2022, 1, 1).plusDays(value))
    }
    val pendingClaims: Seq[PendingClaim] = (1 to 2).map { value =>
      PendingClaim(s"NDRC-${2000 + value}", LocalDate.of(2021, 2, 1).plusDays(value), LocalDate.of(2022, 1, 1).plusDays(value))
    }
    val inProgressClaim: Seq[InProgressClaim] = (1 to 2).map { value =>
      InProgressClaim(s"NDRC-${3000 + value}", LocalDate.of(2021, 2, 1).plusDays(value), newMessage = true)
    }
    Future.successful(closedClaims ++ pendingClaims ++ inProgressClaim)
  }

}
