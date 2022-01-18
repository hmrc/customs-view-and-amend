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

import models.{Claim, ClosedClaim, InProgressClaim, PendingClaim}
import models.ClosedClaim

import java.time.LocalDate
import scala.concurrent.Future

class FinancialsApiConnector {

  def getClaims(): Future[Seq[Claim]] =
    Future.successful(Seq(
      ClosedClaim("NDRC-6666", LocalDate.of(2021, 2, 1), LocalDate.of(2022, 1, 1)),
      ClosedClaim("NDRC-4592", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 5, 1)),
      ClosedClaim("NDRC-8318", LocalDate.of(2021, 4, 1), LocalDate.of(2022, 2, 1)),
      ClosedClaim("NDRC-2318", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 9, 1)),
      ClosedClaim("NDRC-8496", LocalDate.of(2021, 6, 1), LocalDate.of(2022, 7, 1)),
      PendingClaim("NDRC-1965", LocalDate.of(2021, 1, 1), newMessage = true),
      PendingClaim("NDRC-7321", LocalDate.of(2021, 3, 1), newMessage = false),
      PendingClaim("NDRC-3398", LocalDate.of(2021, 4, 1), newMessage = true),
      PendingClaim("NDRC-7792", LocalDate.of(2021, 1, 1), newMessage = false),
      PendingClaim("NDRC-3216", LocalDate.of(2021, 2, 1), newMessage = true),
      PendingClaim("NDRC-7876", LocalDate.of(2021, 3, 1), newMessage = false),
      PendingClaim("NDRC-1294", LocalDate.of(2021, 7, 1), newMessage = true),
      InProgressClaim("NDRC-7935", LocalDate.of(2021, 1, 1), newMessage = true),
      InProgressClaim("NDRC-8975", LocalDate.of(2021, 3, 1), newMessage = true),
      InProgressClaim("NDRC-3789", LocalDate.of(2021, 7, 1), newMessage = true),
      InProgressClaim("NDRC-3753", LocalDate.of(2021, 8, 1), newMessage = true),
      InProgressClaim("NDRC-4567", LocalDate.of(2021, 1, 1), newMessage = true),
      InProgressClaim("NDRC-0476", LocalDate.of(2021, 6, 1), newMessage = true),
      InProgressClaim("NDRC-8874", LocalDate.of(2021, 8, 1), newMessage = true)
    ))


}
