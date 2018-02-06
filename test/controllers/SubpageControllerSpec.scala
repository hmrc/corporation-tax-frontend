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
import models.{CtAccountSummary, CtEnrolment, CtNoData}
import models.requests.{AuthenticatedRequest, ServiceInfoRequest}
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.CtService
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.domain.CtUtr
import views.html.partials.account_summary
import views.html.subpage

import scala.concurrent.Future

class SubpageControllerSpec extends ControllerSpecBase with MockitoSugar {


  val mockCtService: CtService = mock[CtService]

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new SubpageController(frontendAppConfig, mockCtService, messagesApi, FakeAuthAction, FakeServiceInfoAction)

  def requestWithEnrolments(enrolments: Enrolments): ServiceInfoRequest[AnyContent] = {
    ServiceInfoRequest[AnyContent](AuthenticatedRequest(FakeRequest(), "", enrolments), HtmlFormat.empty)
  }

  val authEnrolment = Enrolment("IR-CT").withIdentifier("UTR", "utr")
  val ctEnrolment = CtEnrolment(CtUtr("utr"), isActivated = true)

  val fakeRequestWithEnrolments = requestWithEnrolments(Enrolments(Set(authEnrolment)))

  def accountSummary(balanceInformation: String) = account_summary(balanceInformation, frontendAppConfig)(fakeRequest, messages)

  def viewAsString(balanceInformation: String = "") =
    subpage(frontendAppConfig, None, accountSummary(balanceInformation))(HtmlFormat.empty)(fakeRequestWithEnrolments, messages).toString

  "Subpage Controller" must {
    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad(fakeRequestWithEnrolments)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(balanceInformation = "No balance information to display")
    }
  }

  "Subpage Controller - getEnrolment" must {
    "pull the CT enrolment out of the auth record" in {
      controller().getEnrolment(fakeRequestWithEnrolments) mustBe Some(ctEnrolment)

      val requestNotActivated = requestWithEnrolments(Enrolments(Set(authEnrolment.copy(state = "NotYetActivated"))))
      controller().getEnrolment(requestNotActivated) mustBe Some(ctEnrolment.copy(isActivated = false))
    }
  }

  "Subpage Controller - getAccountSummaryView" when {
    "there is no account summary data" should {
      "return 'No Balance information to display'" in {
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any())).thenReturn(Future.successful(CtNoData))
        controller().getAccountSummaryView(fakeRequestWithEnrolments)
      }
    }
  }

}




