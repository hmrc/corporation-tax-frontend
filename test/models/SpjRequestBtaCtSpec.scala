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

import base.SpecBase
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDate

class SpjRequestBtaCtSpec extends SpecBase {

  val amount = 12345L
  val returnUrl = "return.com"
  val backUrl = "back.com"
  val utr = "012345678"
  val dueDate = "2020-09-01"

  val model: SpjRequestBtaCt = SpjRequestBtaCt(amount, returnUrl, backUrl, utr, dueDate)

  val modelJson: JsObject = Json.obj(
    "amountInPence" -> amount,
    "returnUrl" -> returnUrl,
    "backUrl" -> backUrl,
    "utr" -> utr,
    "dueDate" -> dueDate,
  )

  "localDateToIsoString" when {

    "date is a leap day" must {

      "convert local date to string in ISO date format" in {

        val localDate: LocalDate = LocalDate.of(2024, 2, 29)

        val expectedResult = "2024-02-29"
        val actualResult = SpjRequestBtaCt.localDateToIsoString(localDate)

        expectedResult mustBe actualResult
      }
    }

    "date is not a leap day" must {

      "convert local date to string in ISO date format" in {

        val localDate: LocalDate = LocalDate.of(2023, 9, 1)

        val expectedResult = "2023-09-01"
        val actualResult = SpjRequestBtaCt.localDateToIsoString(localDate)

        expectedResult mustBe actualResult
      }
    }
  }

  "toAmountInPence" when {

    "amount is zero" must {

      "convert to Long in pence" in {

        val amount: BigDecimal = BigDecimal(0)

        val expectedResult: Long = 0L
        val actualResult = SpjRequestBtaCt.toAmountInPence(amount)

        expectedResult mustBe actualResult
      }
    }

    "amount is pence" must {

      "convert to Long in pence" in {

        val amount: BigDecimal = BigDecimal(0.01)

        val expectedResult: Long = 1L
        val actualResult = SpjRequestBtaCt.toAmountInPence(amount)

        expectedResult mustBe actualResult
      }
    }

    "amount is very large" must {

      "convert to Long in pence" in {

        val amount: BigDecimal = BigDecimal(999999999)

        val expectedResult: Long = 99999999900L
        val actualResult = SpjRequestBtaCt.toAmountInPence(amount)

        expectedResult mustBe actualResult
      }
    }
  }

  "read from json" must {

    "return valid model" in {

      val expectedResult = model
      val actualResult = modelJson.as[SpjRequestBtaCt]

      expectedResult mustBe actualResult
    }
  }

  "write to json" must {

    "return valid json" in {

      val expectedResult = modelJson
      val actualResult = Json.toJson(model)

      expectedResult mustBe actualResult
    }
  }
}
