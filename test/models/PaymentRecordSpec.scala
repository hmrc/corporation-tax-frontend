/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import base.SpecBase
import org.joda.time.DateTime
import play.api.i18n.Messages
import play.api.test.FakeRequest

class PaymentRecordSpec extends SpecBase{
  "A PaymentRecord object" when {

    implicit val messages: Messages = messagesApi.preferred(FakeRequest())

    val testDate: DateTime = DateTime.now()

    "isValid is called" should{

      "return true when the created date is less than 7 days old" in{
        val testRecord = PaymentRecord(
          reference = "testRef",
          amountInPence = 1,
          status = Created,
          createdOn = testDate.minusDays(7).plusSeconds(1).toString(),
          taxType = "corporation-tax"
        )
        testRecord.isValid(testDate) mustBe true
      }

      "return false when the created date is more than 7 days old" in{
        val testRecord = PaymentRecord(
          reference = "testRef",
          amountInPence = 1,
          status = Created,
          createdOn = testDate.minusDays(7).minusSeconds(1).toString(),
          taxType = "corporation-tax"
        )
        testRecord.isValid(testDate) mustBe false
      }

      "return false when the created date is not readable" in{
        val testRecord = PaymentRecord(
          reference = "testRef",
          amountInPence = 1,
          status = Created,
          createdOn = "invalid date",
          taxType = "corporation-tax"
        )
        testRecord.isValid(testDate) mustBe false
      }
    }

    "isSuccessful is called" should {
      "return true when the status is Successful" in {
        val testRecord = PaymentRecord(
          reference = "testRef",
          amountInPence = 1,
          status = Successful,
          createdOn = "invalid date",
          taxType = "corporation-tax"
        )
        testRecord.isSuccessful mustBe true
      }

      "return false when the status is not Successful" in {
        val testRecord = PaymentRecord(
          reference = "testRef",
          amountInPence = 1,
          status = Created,
          createdOn = "invalid date",
          taxType = "corporation-tax"
        )
        testRecord.isSuccessful mustBe false
      }
    }

    "dateFormatted is called" should {
      "return the formatted date" in {
        val testRecord = PaymentRecord(
          reference = "testRef",
          amountInPence = 1,
          status = Created,
          createdOn = "2019-04-05T17:25:20.877+01:00",
          taxType = "corporation-tax"
        )
        testRecord.dateFormatted() mustBe "5 April 2019"
      }
    }
  }
}
