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

import play.api.libs.json.{Format, Json}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class SpjRequestBtaCt(amountInPence: Long,
                           returnUrl: String,
                           backUrl: String,
                           utr: String,
                           dueDate: String
                          )

object SpjRequestBtaCt {
  implicit val format: Format[SpjRequestBtaCt] = Json.format[SpjRequestBtaCt]

  def localDateToIsoString(localDate: LocalDate): String = {
    localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
  }

  def toAmountInPence(amountInPounds: BigDecimal): Long = (amountInPounds * 100).toLong

  implicit val localDateOrdering: Ordering[LocalDate] = (x: LocalDate, y: LocalDate) => x compareTo y
}