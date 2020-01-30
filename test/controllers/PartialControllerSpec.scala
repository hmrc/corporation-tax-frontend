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

import controllers.actions._
import models._
import org.mockito.Matchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.{Html, HtmlFormat}
import services.CtCardBuilderService
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.Upstream5xxResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class PartialControllerSpec extends ControllerSpecBase with MockitoSugar {

  trait LocalSetup {
    lazy val accountSummary = Html("Account Summary")
    lazy val mockCtCardBuilderService: CtCardBuilderService = mock[CtCardBuilderService]
    lazy val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
    lazy val c: PartialController = new PartialController(messagesApi, mockAuthAction, mockedServiceInfoAction,
      mockAccountSummaryHelper, frontendAppConfig, mockCtCardBuilderService)

    lazy val testCard: Card = Card(
      title = "CT",
      description = "CT Card",
      referenceNumber = "123456789",
      primaryLink = Some(
        Link(
          id = "ct-account-details-card-link",
          title = "CT",
          href = "http://someTestUrl",
          ga = "link - click:CT cards:More CT details"
        )
      ),
      messageReferenceKey = Some(""),
      paymentsPartial = Some(""),
      returnsPartial = Some("")
    )

    when(mockAccountSummaryHelper.getAccountSummaryView(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(accountSummary))
  }

  "Calling PartialController.getCard" must {
    "return 200 in json format when asked to get a card and the call to the backend succeeds" in new LocalSetup {
      when(mockCtCardBuilderService.buildCtCard()(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(testCard))
      setupFakeAuthAction
      setupFakeServiceAction(HtmlFormat.empty)

      val result: Future[Result] = c.getCard(fakeRequest)

      contentType(result) mustBe Some("application/json")
      status(result) mustBe OK
    }

    "return an error status when asked to get a card and the call to the backend fails" in new LocalSetup {
      when(mockCtCardBuilderService.buildCtCard()(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.failed(Upstream5xxResponse("", 500, 500)))
      setupFakeAuthAction
      setupFakeServiceAction(HtmlFormat.empty)
      val result: Future[Result] = c.getCard(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

}
