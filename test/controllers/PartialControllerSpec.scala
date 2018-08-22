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

import controllers.actions._
import models.CtNoData
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import play.twirl.api.Html
import services.CtService
import uk.gov.hmrc.domain.CtUtr
import views.html.partial

import scala.concurrent.Future


class PartialControllerSpec extends ControllerSpecBase with MockitoSugar {


  val accountSummary = Html("Account Summary")
  val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  when(mockAccountSummaryHelper.getAccountSummaryView(Matchers.any())(Matchers.any())).thenReturn(Future.successful(accountSummary))

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new PartialController(messagesApi, FakeAuthAction, FakeServiceInfoAction, mockAccountSummaryHelper, frontendAppConfig)

  val brokenAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  when (brokenAccountSummaryHelper.getAccountSummaryView(Matchers.any())).thenReturn(Future.failed(new Throwable()))
  def brokenController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new PartialController(messagesApi, FakeAuthAction, FakeServiceInfoAction, brokenAccountSummaryHelper, frontendAppConfig)

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
  }
}




