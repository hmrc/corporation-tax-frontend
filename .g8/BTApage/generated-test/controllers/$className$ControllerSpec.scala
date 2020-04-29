package controllers

import controllers.actions._
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.$className;format="decap"$

class $className$ControllerSpec extends SpecBase {

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new $className$Controller(frontendAppConfig, messagesApi, FakeAuthAction, FakeServiceInfoAction)

  def viewAsString() = $className;format="decap"$(frontendAppConfig)(HtmlFormat.empty)(fakeRequest, messages).toString

  "$className$ Controller" must {

    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }
  }
}




