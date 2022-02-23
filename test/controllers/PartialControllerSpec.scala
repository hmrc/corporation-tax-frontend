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

import base.{FakeAuthAction, FakeServiceInfoAction, SpecBase}
import controllers.actions.ServiceInfoAction
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{MessagesControllerComponents, PlayBodyParsers, Result}
import play.api.test.Helpers._
import play.twirl.api.Html
import services.CtCardBuilderService
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PartialControllerSpec extends SpecBase with MockitoSugar with GuiceOneAppPerSuite {

  val fakeAuth = new FakeAuthAction(app.injector.instanceOf[PlayBodyParsers])
  val fakeServiceInfo: ServiceInfoAction = FakeServiceInfoAction

  trait LocalSetup {
    val accountSummary: Html = Html("Account Summary")
    val mockCtCardBuilderService: CtCardBuilderService = mock[CtCardBuilderService]
    val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
    val c: PartialController =
      new PartialController(
        app.injector.instanceOf[MessagesControllerComponents],
        fakeAuth,
        fakeServiceInfo,
        mockAccountSummaryHelper,
        frontendAppConfig,
        mockCtCardBuilderService
      )

    val testCard: Card = Card(
      title = "CT",
      description = "CT Card",
      referenceNumber = "123456789",
      primaryLink = Some(
        Link(
          id = "ct-account-details-card-link",
          title = "CT",
          href = "http://someTestUrl"
        )
      ),
      messageReferenceKey = Some(""),
      paymentsPartial = Some(""),
      returnsPartial = Some("")
    )

    when(mockAccountSummaryHelper.getAccountSummaryView(any())(any(), any()))
        .thenReturn(Future.successful(accountSummary))
  }

  "Calling PartialController.getCard" must {
    "return 200 in json format when asked to get a card and the call to the backend succeeds" in new LocalSetup {
      when(mockCtCardBuilderService.buildCtCard()(any(), any(), any()))
        .thenReturn(Future.successful(testCard))

      val result: Future[Result] = c.getCard(fakeRequest)

      contentType(result) mustBe Some("application/json")
      status(result) mustBe OK
    }

    "return an error status when asked to get a card and the call to the backend fails" in new LocalSetup {
      when(mockCtCardBuilderService.buildCtCard()(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result: Future[Result] = c.getCard(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

}
