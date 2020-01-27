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

package controllers

import connectors.models._
import connectors.payments.{NextUrl, PaymentConnector}
import controllers.actions._
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import services.CtService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentStartControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  private val testAccountBalance = CtAccountBalance(Some(0.0))
  private val testCtData = CtData(CtAccountSummaryData(Some(testAccountBalance)))
  private val testCtDataNoAccountBalance = CtData(CtAccountSummaryData(None))
  private val testPayUrl = "https://www.tax.service.gov.uk/pay/12345/choose-a-way-to-pay"

  private val mockPayConnector: PaymentConnector = mock[PaymentConnector]
  when(mockPayConnector.ctPayLink(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(NextUrl(testPayUrl)))

  val mockCtService = mock[CtService]

  def buildController = new PaymentStartController(frontendAppConfig, mockPayConnector, FakeAuthAction, mockCtService, messagesApi)

  def setupMockCtService(testModel: Either[CtAccountFailure, Option[CtData]] = Right(Some(testCtData))): Unit = {
    when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testModel))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPayConnector)
    reset(mockCtService)
  }

  "Payment Controller" must {

    "return See Other and a NextUrl for a GET with the correct user information available" in {
      setupMockCtService(Right(Some(testCtData)))
      when(mockPayConnector.ctPayLink(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(NextUrl(testPayUrl)))
      val result: Future[Result] = buildController.makeAPayment(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(testPayUrl)
    }

    "return Bad Request and the error page when the user has no account balance" in {
      setupMockCtService(Right(Some(testCtDataNoAccountBalance)))
      val result: Future[Result] = buildController.makeAPayment(fakeRequest)
      contentType(result) mustBe Some("text/html")
      status(result) mustBe BAD_REQUEST
    }

    "return Bad Request and the error page when CtGenericError is returned " in {
      setupMockCtService(Left(CtGenericError))
      val result: Future[Result] = buildController.makeAPayment(fakeRequest)
      contentType(result) mustBe Some("text/html")
      status(result) mustBe BAD_REQUEST
    }
  }
}
