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

package controllers

import connectors.models._
import connectors.payments.{NextUrl, PaymentConnector}
import controllers.actions._
import models._
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import services.CtServiceInterface
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class PaymentStartControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val testAccountBalance = CtAccountBalance(Some(0.0))
  private val testCtData = CtData(CtAccountSummaryData(Some(testAccountBalance)))
  private val testCtDataNoAccountBalance = CtData(CtAccountSummaryData(None))
  private val testPayUrl = "https://www.tax.service.gov.uk/pay/12345/choose-a-way-to-pay"

  private val mockPayConnector: PaymentConnector = mock[PaymentConnector]
  when(mockPayConnector.ctPayLink(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(NextUrl(testPayUrl)))

  class TestCtService(testModel: CtAccountSummary) extends CtServiceInterface {
    override def fetchCtModel(ctEnrolmentOpt: Option[CtEnrolment])
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[CtAccountSummary] =
      Future(testModel)
  }

  class BrokenVatService extends CtServiceInterface {
    override def fetchCtModel(ctEnrolmentOpt: Option[CtEnrolment])
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[CtAccountSummary] =
      Future.failed(new Throwable())
  }

  def buildController(ctService: CtServiceInterface) = new PaymentStartController(
    frontendAppConfig, mockPayConnector, FakeAuthAction, ctService, messagesApi)

  def customController(testModel: CtAccountSummary = testCtData): PaymentStartController = {
    buildController(new TestCtService(testModel))
  }

  def brokenController: PaymentStartController = buildController(new BrokenVatService)

  "Payment Controller" must {

    "return See Other and a NextUrl for a GET with the correct user information available" in {
      val result: Future[Result] = customController().makeAPayment(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(testPayUrl)
    }

    "return Bad Request and the error page when the call to the backend fails" in {
      val result: Future[Result] = brokenController.makeAPayment(fakeRequest)
      contentType(result) mustBe Some("text/html")
      status(result) mustBe BAD_REQUEST
    }

    "return Bad Request and the error page when the user has no account balance" in {
      val result: Future[Result] = customController(testCtDataNoAccountBalance).makeAPayment(fakeRequest)
      contentType(result) mustBe Some("text/html")
      status(result) mustBe BAD_REQUEST
    }

    "return Bad Request and the error page when the user has erroneous vat data " in {
      val result: Future[Result] = customController(CtGenericError).makeAPayment(fakeRequest)
      contentType(result) mustBe Some("text/html")
      status(result) mustBe BAD_REQUEST
    }
  }
}
