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

import base.{FakeAuthAction, FakeServiceInfoAction, SpecBase}
import controllers.actions.ServiceInfoAction
import models._
import models.requests.{AuthenticatedRequest, ServiceInfoRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, MessagesControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.{Html, HtmlFormat}
import models.CtUtr
import views.ViewSpecBase
import views.html.subpage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubpageControllerSpec extends SpecBase with MockitoSugar with ScalaFutures with ViewSpecBase {

  val accountSummary: Html = Html("Account Summary")
  val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  val fakeAuth = new FakeAuthAction(app.injector.instanceOf[PlayBodyParsers])
  val fakeServiceInfo: ServiceInfoAction = FakeServiceInfoAction
  val subpage: subpage = app.injector.instanceOf[subpage]

  when(mockAccountSummaryHelper.getAccountSummaryView(any())(any(), any()))
    .thenReturn(Future.successful(accountSummary))

  def controller() = new SubpageController(
    frontendAppConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    fakeAuth,
    fakeServiceInfo,
    subpage,
    mockAccountSummaryHelper
  )

  val ctEnrolment: CtEnrolment = CtEnrolment(CtUtr("utr"), isActivated = true)

  def requestWithEnrolment(activated: Boolean): ServiceInfoRequest[AnyContent] = {
    ServiceInfoRequest[AnyContent](AuthenticatedRequest(FakeRequest(), "", ctEnrolment), HtmlFormat.empty)
  }

  val fakeRequestWithEnrolments: ServiceInfoRequest[AnyContent] = requestWithEnrolment(activated = true)

  val expected: String = subpage(frontendAppConfig, ctEnrolment, accountSummary)(HtmlFormat.empty)(fakeRequestWithEnrolments, messages).toString

  "Subpage Controller" must {
    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad(fakeRequestWithEnrolments)

      status(result) mustBe OK
      contentAsString(result) mustBe expected
    }
  }

}




