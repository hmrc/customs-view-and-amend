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

package models

import play.api.libs.json.{Json, OFormat}

case class AllClaims(pendingClaims: Seq[PendingClaim],
                     inProgressClaims: Seq[InProgressClaim],
                     closedClaims: Seq[ClosedClaim]) {

  def collectAll: Seq[Claim] = pendingClaims ++ inProgressClaims ++ closedClaims

  def findClaim(query: String): Seq[Claim] = collectAll.filter(claim =>
    claim.caseNumber.toUpperCase == query.toUpperCase ||
      claim.declarationId.toUpperCase == query.toUpperCase
  )

}

object AllClaims {
  implicit val format: OFormat[AllClaims] = Json.format[AllClaims]
}
