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

import play.api.libs.json.{Json, OFormat}

case class AllClaims(
  pendingClaims: Seq[PendingClaim],
  inProgressClaims: Seq[InProgressClaim],
  closedClaims: Seq[ClosedClaim]
) {

  def nonEmpty: Boolean = pendingClaims.nonEmpty || inProgressClaims.nonEmpty || closedClaims.nonEmpty

  /** Searches for a claim based on the user's query */
  def searchForClaim(query: String): Seq[Claim] = {
    val predicate: Claim => Boolean = claim =>
      claim.caseNumber.toUpperCase == query.toUpperCase ||
        claim.declarationId.toUpperCase == query.toUpperCase

    pendingClaims.filter(predicate) ++
      inProgressClaims.filter(predicate) ++
      closedClaims.filter(predicate)
  }

  /** Finds claim by its caseNumber */
  def findByCaseNumber(caseNumber: String): Option[Claim] =
    pendingClaims
      .find(_.caseNumber == caseNumber)
      .orElse(inProgressClaims.find(_.caseNumber == caseNumber))
      .orElse(closedClaims.find(_.caseNumber == caseNumber))

}

object AllClaims {
  implicit val format: OFormat[AllClaims] = Json.format[AllClaims]
}
