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

sealed trait Error {
  type Value
  val value: Value
  def exception: Throwable
}

object Error {
  def apply(t: Throwable): Error = new Error {
    final override type Value = Throwable
    final override val value: Throwable = t
    final override def exception          = value
  }

  def apply(message: String): Error = new Error {
    final override type Value = String
    final override val value: String = message
    final override def exception       = new Exception(message)
  }
}
