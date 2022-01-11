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

package controllers

import base.{FakeAuthAction, SpecBase}
import connectors.payments.{NextUrl, PaymentConnector}
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{MessagesControllerComponents, PlayBodyParsers, Result}
import play.api.test.Helpers._
import services.CtService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentStartControllerSpec extends SpecBase with MockitoSugar {

  val testAccountBalance: CtAccountBalance = CtAccountBalance(Some(0.0))
  val testCtData: CtData = CtData(CtAccountSummaryData(Some(testAccountBalance)))
  val testCtDataNoAccountBalance: CtData = CtData(CtAccountSummaryData(None))
  val testPayUrl: String = "https://www.tax.service.gov.uk/pay/12345/choose-a-way-to-pay"

  val mockPayConnector: PaymentConnector = mock[PaymentConnector]
  val mockCtService: CtService = mock[CtService]
  val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val fakeAuth = new FakeAuthAction(app.injector.instanceOf[PlayBodyParsers])

  val controller: PaymentStartController = new PaymentStartController(
      frontendAppConfig, mockPayConnector, fakeAuth, mockCtService, mcc
  )

  "Payment Controller" must {
    "return See Other and a NextUrl for a GET with the correct user information available" in {
      when(mockPayConnector.ctPayLink(any())(any(), any()))
        .thenReturn(Future.successful(NextUrl(testPayUrl)))

      when(mockCtService.fetchCtModel(any())(any(),any()))
        .thenReturn(Future.successful(Right(Some(testCtData))))

      val result: Future[Result] = controller.makeAPayment(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(testPayUrl)
    }

    "return Bad Request and the error page when the user has no account balance" in {
      when(mockPayConnector.ctPayLink(any())(any(), any()))
        .thenReturn(Future.successful(NextUrl(testPayUrl)))

      when(mockCtService.fetchCtModel(any())(any(),any()))
        .thenReturn(Future.successful(Right(Some(testCtDataNoAccountBalance))))

      val result: Future[Result] = controller.makeAPayment(fakeRequest)
//      contentType(result) mustBe Some("text/html")
      status(result) mustBe BAD_REQUEST
    }

    "return Bad Request and the error page when CtGenericError is returend " in {
      when(mockPayConnector.ctPayLink(any())(any(), any()))
        .thenReturn(Future.successful(NextUrl(testPayUrl)))

      when(mockCtService.fetchCtModel(any())(any(),any()))
        .thenReturn(Future.successful(Left(CtGenericError)))

      val result: Future[Result] = controller.makeAPayment(fakeRequest)
//      contentType(result) mustBe Some("text/html")
      status(result) mustBe BAD_REQUEST
    }
  }
}
