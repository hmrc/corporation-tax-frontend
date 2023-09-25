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

package constants

import models.{CtAccountBalance, CtAccountSummaryData, CtData}

import java.time.LocalDate

object CtAccountSummaryConstants {

  val amount: BigDecimal = BigDecimal(123.45)


  val effectiveDueDate: LocalDate = LocalDate.parse("2023-09-22")

  def ctAccountSummary(amount: BigDecimal = amount): CtAccountSummaryData =
    CtAccountSummaryData(
      accountBalance = Some(CtAccountBalance(Some(amount))),
      effectiveDueDate = effectiveDueDate
    )

  val ctAccountSummary: CtAccountSummaryData = ctAccountSummary()

  def ctData(amount: BigDecimal = amount): CtData =
    CtData(ctAccountSummary(amount))

  val ctData: CtData = ctData()

}
