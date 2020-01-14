/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.payments.PaymentHistoryConnectorInterface
import models.payments.PaymentStatus._
import models.payments._
import models.{CtEnrolment, PaymentRecordFailure}
import org.joda.time.DateTime
import org.scalatest.TestData
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import services.PaymentHistoryService
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentHistoryConnectorNotFound extends PaymentHistoryConnectorInterface {
  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier) = Future.successful(Right(Nil))
}

class PaymentHistoryParseError extends PaymentHistoryConnectorInterface {
  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier) = Future.successful(Left("unable to parse data from payment api"))
}

class PaymentHistoryConnectorSingleRecord(val date: String = "2018-10-20T08:00:00.000", status: PaymentStatus = Successful) extends PaymentHistoryConnectorInterface {
  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier) = Future.successful(
    Right(List(
      CtPaymentRecord(
        reference = "reference number",
        amountInPence = 100,
        status = status,
        createdOn = date,
        taxType = "tax type"
      )
    )
    ))
}

class PaymentHistoryConnectorMultiple extends PaymentHistoryConnectorInterface {
  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier) =
    Future.successful(Right(List(
      CtPaymentRecord(
        reference = "reference number",
        amountInPence = 150,
        status = Successful,
        createdOn = "2018-10-19T08:00:00.000",
        taxType = "tax type"
      ),
      CtPaymentRecord(
        reference = "reference number",
        amountInPence = 100,
        status = Successful,
        createdOn = "2018-10-13T07:59:00.000",
        taxType = "tax type"
      )
    )))
}

class PaymentsHistoryServiceSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerTest {

  class buildService(mockedConnector: PaymentHistoryConnectorInterface) {
    def injector: Injector = app.injector

    def frontendAppConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

    def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

    def messages: Messages = messagesApi.preferred(fakeRequest)

    lazy val testService = new PaymentHistoryService(mockedConnector, frontendAppConfig)
  }

  def fakeRequest = FakeRequest("", "")

  override def newAppForTest(testData: TestData): Application = {
    val builder = new GuiceApplicationBuilder()
    testData.name match {
      case a if a.matches("^.*toggle set to false.*") => {
        builder.configure(Map("toggles.ct-payment-history" -> false)).build()
      }
      case _ => {
        builder.configure(Map("toggles.ct-payment-history" -> true)).build()
      }
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val date = new DateTime("2018-10-20T08:00:00.000")

  "PaymentHistoryServiceSpec" when {

    "getPayments is called and getSAPaymentHistory toggle set to false" should {

      "return Nil" in new buildService(new PaymentHistoryConnectorSingleRecord()) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Right(Nil)
      }

    }

    "getPayments is called and getSAPaymentHistory toggle set to true" should {

      "return payment history when valid payment history is returned" in new buildService(new PaymentHistoryConnectorSingleRecord()) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Right(List(
          PaymentRecord(
            reference = "reference number",
            amountInPence = 100,
            createdOn = new DateTime("2018-10-20T08:00:00.000"),
            taxType = "tax type"
          )
        ))
      }

      "return payment history when payments fall within and outside 7 days" in new buildService(new PaymentHistoryConnectorMultiple()) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Right(List(
          PaymentRecord(
            reference = "reference number",
            amountInPence = 150,
            createdOn = new DateTime("2018-10-19T08:00:00.000"),
            taxType = "tax type"
          )
        ))
      }

      "not return payment history when status is not Successful" in new buildService(new PaymentHistoryConnectorSingleRecord(status = Invalid)) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Right(Nil)
      }

      "not return payment history when payment falls outside of 7 days" in new buildService(
        new PaymentHistoryConnectorSingleRecord("2018-10-13T07:59:00.000")
      ) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Right(Nil)
      }

      "return Nil when date is invalid format" in new buildService(new PaymentHistoryConnectorSingleRecord("invalid-date")) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Right(Nil)
      }

      "return Nil when payment history could not be found" in new buildService(new PaymentHistoryConnectorNotFound) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Right(Nil)
      }

      "return Left(PaymentRecordFailure) when connector fails to parse" in new buildService(new PaymentHistoryParseError) {
        testService.getPayments(CtEnrolment(CtUtr("utr"), true), date).futureValue mustBe Left(PaymentRecordFailure)
      }

    }

  }

}
