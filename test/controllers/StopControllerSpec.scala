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

import connectors.FakeDataCacheConnector
import controllers.actions._
import forms.StopFormProvider
import models.Stop
import play.api.data.Form
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import repositories.SessionRepository
import utils.{CascadeUpsert, FakeNavigator}
import views.html.stop

class StopControllerSpec extends ControllerSpecBase {

  def onwardRoute = routes.SubpageController.onPageLoad()

  val formProvider = new StopFormProvider()
  val form = formProvider()
  val mockSessionRepository = mock[SessionRepository]
  val mockCascadeUpsert = mock[CascadeUpsert]

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new StopController(
      frontendAppConfig,
      messagesApi,
      new FakeDataCacheConnector(mockSessionRepository, mockCascadeUpsert),
      new FakeNavigator(desiredRoute = onwardRoute), mockAuthAction,
      mockedServiceInfoAction,
      formProvider
    )

  def viewAsString(form: Form[_] = form) = stop(frontendAppConfig, form)(HtmlFormat.empty)(fakeRequest, messages).toString

  "Stop Controller" must {

    "return OK and the correct view for a GET" in {
      setupFakeAuthAction
      setupFakeServiceAction(HtmlFormat.empty)
      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "redirect to the next page when valid data is submitted" in {
      setupFakeAuthAction
      setupFakeServiceAction(HtmlFormat.empty)
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", Stop.options.head.value))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      setupFakeAuthAction
      setupFakeServiceAction(HtmlFormat.empty)
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm = form.bind(Map("value" -> "invalid value"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return OK if no existing data is found" in {
      setupFakeAuthAction
      setupFakeServiceAction(HtmlFormat.empty)
      val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

      status(result) mustBe OK
    }

    "redirect to next page when valid data is submitted and no existing data is found" in {
      setupFakeAuthAction
      setupFakeServiceAction(HtmlFormat.empty)
      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", (Stop.options.head.value)))
      val result = controller(dontGetAnyData).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }
  }
}
