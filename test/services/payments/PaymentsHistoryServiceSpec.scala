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

package services.payments

import config.FrontendAppConfig
import connectors.payments.PaymentHistoryConnector
import models.payments.PaymentStatus._
import models.payments._
import models.{CtEnrolment, CtUtr, PaymentRecordFailure}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import services.PaymentHistoryService
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateUtil

import java.time.{LocalDateTime, OffsetDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class PaymentsHistoryServiceSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerTest with MockitoSugar with DateUtil {

  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockConnector: PaymentHistoryConnector = mock[PaymentHistoryConnector]

  val testService = new PaymentHistoryService(mockConnector, mockConfig)(global)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy val date: OffsetDateTime = LocalDateTime.parse("2018-10-20T08:00:00.000").utcOffset

  "PaymentHistoryServiceSpec" when {
    "getPayments is called and getSAPaymentHistory toggle set to true" should {
      "return payment history when valid payment history is returned" in {
        when(mockConnector.get(any())(any()))
          .thenReturn(Future.successful(
            Right(
              List(CtPaymentRecord(
                reference = "reference number",
                amountInPence = 1,
                status = Successful,
                createdOn = "2018-10-20T08:00:00.000",
                taxType = "tax type")
              )
            )
          ))

        testService.getPayments(CtEnrolment(CtUtr("utr"), isActivated = true), date).futureValue mustBe Right(List(
          PaymentRecord(
            reference = "reference number",
            amountInPence = 1,
            createdOn = date,
            taxType = "tax type"
          )
        ))
      }

      "return payment history when payments fall within and outside 7 days" in {
        when(mockConnector.get(any())(any()))
          .thenReturn(Future.successful(Right(List(
            CtPaymentRecord(
              reference = "reference number",
              amountInPence = 3,
              status = Successful,
              createdOn = "2018-10-19T08:00:00.000",
              taxType = "tax type"
            ),
            CtPaymentRecord(
              reference = "reference number",
              amountInPence = 1,
              status = Successful,
              createdOn = "2018-10-13T07:59:00.000",
              taxType = "tax type"
            )
          ))))

        testService.getPayments(CtEnrolment(CtUtr("utr"), isActivated = true), date).futureValue mustBe Right(List(
          PaymentRecord(
            reference = "reference number",
            amountInPence = 3,
            createdOn = date.minusDays(1),
            taxType = "tax type"
          )
        ))
      }

      "not return payment history when status is not Successful" in {
        when(mockConnector.get(any())(any()))
          .thenReturn(Future.successful(
            Right(
              List(CtPaymentRecord(
                reference = "reference number",
                amountInPence = 1,
                status = Invalid,
                createdOn = "2018-10-20T08:00:00.000",
                taxType = "tax type")
              )
            )
          ))

        testService.getPayments(CtEnrolment(CtUtr("utr"), isActivated = true), date).futureValue mustBe Right(Nil)
      }

      "not return payment history when payment falls outside of 7 days" in {
        when(mockConnector.get(any())(any()))
          .thenReturn(Future.successful(
            Right(
              List(CtPaymentRecord(
                reference = "reference number",
                amountInPence = 1,
                status = Successful,
                createdOn = "2018-10-13T07:59:00.000",
                taxType = "tax type")
              )
            )
          ))

        testService.getPayments(CtEnrolment(CtUtr("utr"), isActivated = true), date).futureValue mustBe Right(Nil)
      }

      "return Nil when date is invalid format" in {
        when(mockConnector.get(any())(any()))
          .thenReturn(Future.successful(
            Right(
              List(CtPaymentRecord(
                reference = "reference number",
                amountInPence = 1,
                status = Successful,
                createdOn = "invalid-date",
                taxType = "tax type")
              )
            )
          ))

        testService.getPayments(CtEnrolment(CtUtr("utr"), isActivated = true), date).futureValue mustBe Right(Nil)
      }

      "return Nil when payment history could not be found" in {
        when(mockConnector.get(any())(any()))
          .thenReturn(Future.successful(Right(Nil)))

        testService.getPayments(CtEnrolment(CtUtr("utr"), isActivated = true), date).futureValue mustBe Right(Nil)
      }

      "return Left(PaymentRecordFailure) when connector fails to parse" in {
        when(mockConnector.get(any())(any()))
          .thenReturn(Future.successful(Left("unable to parse data from payment api")))

        testService.getPayments(CtEnrolment(CtUtr("utr"), isActivated = true), date).futureValue mustBe Left(PaymentRecordFailure)
      }
    }
  }

}
