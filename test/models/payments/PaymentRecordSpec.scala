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

package models.payments

import models.requests.AuthenticatedRequest
import models.{CtEnrolment, CtUtr, PaymentRecordFailure}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.test.FakeRequest
import utils.DateUtil
import models.payments.PaymentRecord

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.util.Random

class PaymentRecordSpec extends PlaySpec with GuiceOneServerPerSuite with DateUtil {

  implicit val request: AuthenticatedRequest[_] = AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true))
//  implicit val date = validateAndConvertToPaymentRecord()

  val testReference: String = UUID.randomUUID().toString
  val testAmountInPence: Long = Random.nextLong()
  val currentDateTime: OffsetDateTime = OffsetDateTime.of(2022, 4, 1, 11, 59, 59, 0, ZoneOffset.UTC)
  val testCreatedOn: String = "2022-04-01T11:59:59.000"
  val testTaxType: String = "testTaxType"

  val testPaymentRecord: PaymentRecord = PaymentRecord(
    reference = testReference,
    amountInPence = testAmountInPence,
    createdOn = currentDateTime,
    taxType = testTaxType
  )

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(FakeRequest())

  def testJson(createOn: String): String =
    s"""
       |{
       |  "reference" : "$testReference",
       |  "amountInPence" : $testAmountInPence,
       |  "status" : ${Json.toJson[PaymentStatus](PaymentStatus.Successful).toString()},
       |  "createdOn" : "$createOn",
       |  "taxType" : "$testTaxType"
       |}
    """.stripMargin

  "dateTimeWrites" must {

    "convert OffsetDateTime to String in localDateTime format as this is what BTA expects" in {

      Json.toJson(currentDateTime)(PaymentRecord.dateTimeWrites) mustBe JsString("2022-04-01T11:59:59")
    }
  }

  "format" should {
    "parse the json correctly if the createOn is a valid DateTime" in {
      val expected: PaymentRecord = testPaymentRecord
      Json.fromJson[PaymentRecord](Json.parse(testJson(testCreatedOn))) mustBe JsSuccess(expected)
    }
    "fail to parse if the createOn is an invalid DateTime" in {
      Json.fromJson[PaymentRecord](Json.parse(testJson(""))) mustBe an[JsError]
    }

    "output of the writer should be readable by its own reader" in {
      val init: PaymentRecord = testPaymentRecord
      val writtenJson = Json.toJson(init)
      Json.fromJson[PaymentRecord](writtenJson) mustBe JsSuccess(init)
    }
  }

  "eitherPaymentHistoryFormatter" should {
    type TestE = Either[PaymentRecordFailure.type, List[PaymentRecord]]

    def testJsonString(paymentHistory: TestE): JsValue =
      paymentHistory.right.map(Json.toJson[List[PaymentRecord]](_)).left.map(_ => JsString("Bad Gateway")).merge

    "parse JsArray of PaymentRecord as right" in {
      val altTestReference = "anotherReference"
      val json: JsValue = testJsonString(Right(List(testPaymentRecord, testPaymentRecord.copy(reference = altTestReference))))
      Json.fromJson[TestE](json).get mustBe Right(List(testPaymentRecord, testPaymentRecord.copy(reference = altTestReference)))
    }
    "parse JsArray of any other type(s) asLeft" in {
      val json: JsValue = Json.parse("""[ "string", 1 ]""")
      Json.fromJson[TestE](json).get mustBe Left(PaymentRecordFailure)
    }
    "parse anything else as Left" in {
      val json: JsValue = testJsonString(Left(PaymentRecordFailure))
      Json.fromJson[TestE](json).get mustBe Left(PaymentRecordFailure)
    }
    "for Right(list) output JsArray" in {
      val testCard: TestE = Right(Nil)
      Json.toJson(testCard) mustBe JsArray()
    }
    "for Left(PaymentRecordFailure) output the JsString of Bad Gateway" in {
      val testCard: TestE = Left(PaymentRecordFailure)
      Json.toJson(testCard) mustBe JsString(PaymentRecord.paymentRecordFailureString)
    }
  }

  "PaymentRecord.dateFormatted" should {
    "display the date in d MMMM yyyy format" in {
      val testRecord = testPaymentRecord.copy(createdOn = currentDateTime)
      testRecord.dateFormatted mustBe s"1 April 2022"
    }
  }

  "PaymentRecord.currencyFormatted" should {
    "for whole pounds under 1000 add the £ and no decimals" in {
      val testAmount = 99900
      val testRecord = testPaymentRecord.copy(amountInPence = testAmount)
      testRecord.currencyFormatted mustBe "£999"
    }
    "for whole pounds over 1000 add ," in {
      val testAmount = 100000000
      val testRecord = testPaymentRecord.copy(amountInPence = testAmount)
      testRecord.currencyFormatted mustBe "£1,000,000"
    }
    "for any none zero pence values show 2 decimal places" in {
      val testAmount = 100000010
      val testRecord = testPaymentRecord.copy(amountInPence = testAmount)
      testRecord.currencyFormatted mustBe "£1,000,000.10"
    }
  }

  val ctPaymentRecord: CtPaymentRecord = CtPaymentRecord(
    reference = testReference,
    amountInPence = testAmountInPence,
    status = PaymentStatus.Successful,
    createdOn = testCreatedOn,
    taxType = testTaxType
  )

  "CtPaymentRecord" when {

    "isValid" when {

      "createdOn + 7 days is before currentDateTime" must {

        "return false" in {

          val createdOn = currentDateTime.minusDays(8)

          ctPaymentRecord.isValid(createdOn, currentDateTime) mustBe false
        }
      }

      "createdOn + 7 days equals currentDateTime" must {

        "return false" in {

          val createdOn = currentDateTime.minusDays(7)

          ctPaymentRecord.isValid(createdOn, currentDateTime) mustBe false
        }
      }

      "createdOn + 7 days is after currentDateTime" must {

        "return true" in {

          val createdOn = currentDateTime.minusDays(6)

          ctPaymentRecord.isValid(createdOn, currentDateTime) mustBe true
        }
      }
    }

    "validateAndConvertToPaymentRecord" when {

      "createdOn can be parsed to OffsetDateTime" when {

        "createdOn is valid" when {

          "status is Successful" must {

            "return Some(PaymentRecord)" in {

              val ctPaymentRecordData = CtPaymentRecord(
                reference = testReference,
                amountInPence = testAmountInPence,
                status = PaymentStatus.Successful,
                createdOn = testCreatedOn,
                taxType = testTaxType
              )

              val expectedPaymentRecord = Some(PaymentRecord(
                reference = testReference,
                amountInPence = testAmountInPence,
                createdOn = currentDateTime,
                taxType = testTaxType
              ))

              ctPaymentRecordData.validateAndConvertToPaymentRecord(currentDateTime) mustBe expectedPaymentRecord
            }
          }

          "status is not Successful" must {

            "return Some(PaymentRecord)" in {

              val ctPaymentRecordData = CtPaymentRecord(
                reference = testReference,
                amountInPence = testAmountInPence,
                status = PaymentStatus.Invalid,
                createdOn = testCreatedOn,
                taxType = testTaxType
              )

              ctPaymentRecordData.validateAndConvertToPaymentRecord(currentDateTime) mustBe None
            }
          }
        }

        "createdOn is invalid" when {

          "status is Successful" must {

            "return Some(PaymentRecord)" in {

              val ctPaymentRecordData = CtPaymentRecord(
                reference = testReference,
                amountInPence = testAmountInPence,
                status = PaymentStatus.Successful,
                createdOn = "2022-03-25T11:59:59.000",
                taxType = testTaxType
              )

              ctPaymentRecordData.validateAndConvertToPaymentRecord(currentDateTime) mustBe None
            }
          }

          "status is not Successful" must {

            "return Some(PaymentRecord)" in {

              val ctPaymentRecordData = CtPaymentRecord(
                reference = testReference,
                amountInPence = testAmountInPence,
                status = PaymentStatus.Invalid,
                createdOn = "2022-03-25T11:59:59.000",
                taxType = testTaxType
              )

              ctPaymentRecordData.validateAndConvertToPaymentRecord(currentDateTime) mustBe None
            }
          }
        }
      }

      "createdOn can be parsed to OffsetDateTime" must {

        "return None" in {

          val ctPaymentRecordData = CtPaymentRecord(
            reference = testReference,
            amountInPence = testAmountInPence,
            status = PaymentStatus.Successful,
            createdOn = "NOT A DATE TIME",
            taxType = testTaxType
          )

          ctPaymentRecordData.validateAndConvertToPaymentRecord(currentDateTime) mustBe None
        }
      }
    }
  }
}
