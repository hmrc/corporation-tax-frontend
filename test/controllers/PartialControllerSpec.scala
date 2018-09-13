/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.CtConnector
import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import controllers.actions._
import models.{CtAccountSummary, CtData, CtEnrolment, CtNoData}
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import play.twirl.api.Html
import services.CtService
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.HeaderCarrier
import views.html.partial
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future


class PartialControllerSpec extends ControllerSpecBase with MockitoSugar {


  val accountSummary = Html("Account Summary")
  val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  when(mockAccountSummaryHelper.getAccountSummaryView(Matchers.any())(Matchers.any())).thenReturn(Future.successful(accountSummary))
  val mockCtService:CtService = mock[CtService]
  when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any())).thenReturn(
    Future.successful(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0.0))))))
  )

  class TestCtService(testModel: CtAccountSummary, ctConnector: CtConnector) extends CtService(ctConnector) {
    override def fetchCtModel(ctEnrolmentOpt: Option[CtEnrolment])(implicit headerCarrier: HeaderCarrier): Future[CtAccountSummary] = {
      Future(testModel)
    }

  }
  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new PartialController(messagesApi, FakeAuthAction, FakeServiceInfoAction, mockAccountSummaryHelper, frontendAppConfig, mockCtService)

  def customController(testModel: CtAccountSummary) = {
    new PartialController(messagesApi, FakeAuthAction, FakeServiceInfoAction, mockAccountSummaryHelper, frontendAppConfig,
      new TestCtService(testModel,mock[CtConnector]))
  }

  val brokenCtService: CtService = mock[CtService]
  when(brokenCtService.fetchCtModel(Matchers.any())(Matchers.any())).thenReturn(Future.failed(new Throwable()))
  def brokenController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new PartialController(messagesApi, FakeAuthAction, FakeServiceInfoAction, mockAccountSummaryHelper, frontendAppConfig, brokenCtService)

  def viewAsString() = partial(CtUtr("utr"), accountSummary, frontendAppConfig)(fakeRequest, messages).toString

  "Partial Controller" must {

    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return 200 when asked to get a card and the call to the backend succeeds" in {
      val result = controller().getCard(fakeRequest)

      status(result) mustBe OK
    }

    "return an error status when asked to get a card and the call to the backend fails" in {
      val result = brokenController().getCard(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return a card with the text 'You have nothing to pay' when the balance is 0" in {
      val result =customController(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0.0)))))).getCard(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) must include("\"description\":\"You have nothing to pay\"")
    }

    "return a card with the text 'You owe £x.yz' when the balance is £x.yz" in {
      val result =customController(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(1.2)))))).getCard(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) must include("\"description\":\"You owe £1.20\"")
    }

    "return a card with the text 'You are £x.yz in credit' when the balance is -£x.yz" in {
      val result =customController(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(-1.2)))))).getCard(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) must include("\"description\":\"You are £1.20 in credit\"")
    }
  }
}




