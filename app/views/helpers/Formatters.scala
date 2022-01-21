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

package views.helpers

import play.api.i18n.Messages

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale


trait DateFormatters {
  def dateAsDayMonthAndYear(date: LocalDate)(implicit messages: Messages): String =
    s"${date.getDayOfMonth} ${messages(s"month.${date.getMonthValue}")} ${date.getYear}"
}

trait CurrencyFormatters {
  def formatCurrencyAmount(amount: BigDecimal): String = {
    val numberFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.UK)
    val outputDecimals = if (amount.isWhole()) 0 else 2
    numberFormat.setMaximumFractionDigits(outputDecimals)
    numberFormat.setMinimumFractionDigits(outputDecimals)
    numberFormat.format(amount)
  }

  def formatCurrencyAmountWithLeadingPlus(amount: BigDecimal): String = {
    val formattedAmount = formatCurrencyAmount(amount)
    if (amount > 0) {
      "+" + formattedAmount
    } else {
      formattedAmount
    }
  }
}

object Formatters extends DateFormatters with CurrencyFormatters

