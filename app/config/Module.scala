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

package config

import actions.{AuthenticatedIdentifierAction, IdentifierAction}
import com.google.inject.AbstractModule
import repositories.{ClaimsCache, DefaultClaimsCache, DefaultSearchCache, DefaultUploadedFilesCache, SearchCache, UploadedFilesCache}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()
    bind(classOf[UploadedFilesCache]).to(classOf[DefaultUploadedFilesCache]).asEagerSingleton()
    bind(classOf[SearchCache]).to(classOf[DefaultSearchCache]).asEagerSingleton()
    bind(classOf[ClaimsCache]).to(classOf[DefaultClaimsCache]).asEagerSingleton()
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector]).asEagerSingleton()
  }
}
