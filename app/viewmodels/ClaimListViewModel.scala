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

package viewmodels
import config.AppConfig
import models.{ClosedClaim, InProgressClaim, PendingClaim}

case class PendingClaimListViewModel(pendingClaims: Seq[PendingClaim], page: Option[Int])(implicit appConfig: AppConfig) extends Paginated[PendingClaim] {
  override val allItems: Seq[PendingClaim] = pendingClaims
  override val itemsPerPage: Int = appConfig.itemsPerPage
  override val requestedPage: Int = page.getOrElse(1)
  override val urlForPage: Int => String = e => controllers.routes.ClaimListController.showPendingClaimList(Some(e)).url
}

case class InProgressClaimListViewModel(inProgressClaims: Seq[InProgressClaim], page: Option[Int])(implicit appConfig: AppConfig) extends Paginated[InProgressClaim] {
  override val allItems: Seq[InProgressClaim] = inProgressClaims
  override val itemsPerPage: Int = appConfig.itemsPerPage
  override val requestedPage: Int = page.getOrElse(1)
  override val urlForPage: Int => String = e => controllers.routes.ClaimListController.showInProgressClaimList(Some(e)).url
}

case class ClosedClaimListViewModel(closedClaims: Seq[ClosedClaim], page: Option[Int])(implicit appConfig: AppConfig) extends Paginated[ClosedClaim] {
  override val allItems: Seq[ClosedClaim] = closedClaims
  override val itemsPerPage: Int = appConfig.itemsPerPage
  override val requestedPage: Int = page.getOrElse(1)
  override val urlForPage: Int => String = e => controllers.routes.ClaimListController.showClosedClaimList(Some(e)).url
}
