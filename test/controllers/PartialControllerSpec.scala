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

import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import controllers.actions._
import models.{CtAccountSummary, CtData, CtEnrolment, CtNoData}
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{CtService, CtServiceInterface}
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.HeaderCarrier
import views.html.partial

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class PartialControllerSpec extends ControllerSpecBase with MockitoSugar {

  val accountSummary = Html("Account Summary")
  val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  when(mockAccountSummaryHelper.getAccountSummaryView(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(accountSummary))

  class ZeroBalance extends CtServiceInterface {
    override def fetchCtModel(ctEnrolmentOpt: Option[CtEnrolment])(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[CtAccountSummary] = {
      Future.successful(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0.0))))))
    }
  }

  class TestCtService(testModel: CtAccountSummary) extends CtServiceInterface {
    override def fetchCtModel(ctEnrolmentOpt: Option[CtEnrolment])(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[CtAccountSummary] = {
      Future(testModel)
    }
  }

  val cc = app.injector.instanceOf[MessagesControllerComponents]

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new PartialController(messagesApi, FakeAuthAction(cc.parsers.defaultBodyParser), FakeServiceInfoAction,
      mockAccountSummaryHelper, frontendAppConfig, new ZeroBalance, cc)

  def customController(testModel: CtAccountSummary) = {
    new PartialController(messagesApi, FakeAuthAction(cc.parsers.defaultBodyParser), FakeServiceInfoAction,
      mockAccountSummaryHelper, frontendAppConfig,
      new TestCtService(testModel), cc)
  }

  val brokenCtService: CtService = mock[CtService]
  when(brokenCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Throwable()))
  def brokenController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new PartialController(messagesApi, FakeAuthAction(cc.parsers.defaultBodyParser), FakeServiceInfoAction,
      mockAccountSummaryHelper, frontendAppConfig, brokenCtService, cc)

  def viewAsString() = partial(CtUtr("utr"), accountSummary, frontendAppConfig)(fakeRequest, messages).toString

  "Partial Controller" must {

    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad(fakeRequest)
      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      contentAsString(result) mustBe viewAsString()
    }

    "return 200 when asked to get a card and the call to the backend succeeds" in {
      val result = controller().getCard(fakeRequest)
      contentType(result) mustBe Some("application/json")
      status(result) mustBe OK
    }

    "return an error status when asked to get a card and the call to the backend fails" in {
      val result = brokenController().getCard(fakeRequest)
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return a card with the text 'You have nothing to pay' when the balance is 0" in {
      val result =customController(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0.0)))))).getCard(fakeRequest)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.obj(
        "title" -> "Corporation Tax",
        "description" -> "You have nothing to pay",
        "referenceNumber" -> "utr",
        "primaryLink" -> Json.obj(
          "href" -> "http://localhost:9731/business-account/corporation-tax",
          "ga" -> "link - click:Your business taxes cards:More Corporation Tax details",
          "id" -> "ct-account-details-card-link",
          "title"->"Corporation Tax",
          "external" -> false
        ),
        "additionalLinks" -> Json.arr()
      )
    }

    "return an error status when the return from the backend does not contain usable data" in {
      val result =customController(CtNoData).getCard(fakeRequest)
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return a card with the text 'You owe £x.yz' when the balance is £x.yz" in {
      val result =customController(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(1.2)))))).getCard(fakeRequest)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.obj(
        "title" -> "Corporation Tax",
        "description" -> "You owe £1.20",
        "referenceNumber" -> "utr",
        "primaryLink" -> Json.obj(
          "href" -> "http://localhost:9731/business-account/corporation-tax",
          "ga" -> "link - click:Your business taxes cards:More Corporation Tax details",
          "id" -> "ct-account-details-card-link",
          "title"->"Corporation Tax",
          "external" -> false
        ),
        "additionalLinks" -> Json.arr()
      )
    }

    "return a card with the text 'You are £x.yz in credit' when the balance is -£x.yz" in {
      val result =customController(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(-1.2)))))).getCard(fakeRequest)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.obj(
        "title" -> "Corporation Tax",
        "description" -> "You are £1.20 in credit",
        "referenceNumber" -> "utr",
        "primaryLink" -> Json.obj(
          "href" -> "http://localhost:9731/business-account/corporation-tax",
          "ga" -> "link - click:Your business taxes cards:More Corporation Tax details",
          "id" -> "ct-account-details-card-link",
          "title"-> "Corporation Tax",
          "external" -> false
        ),
        "additionalLinks" -> Json.arr()
      )
    }
  }
}




