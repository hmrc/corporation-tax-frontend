/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import play.api.test.Injecting
import views.behaviours.ViewBehaviours
import views.html.unauthorised

import scala.concurrent.Future

class UnauthorisedControllerSpec extends ViewBehaviours with Injecting {

  implicit val request: Request[AnyContent] = fakeRequest

  lazy val SUT: UnauthorisedController = inject[UnauthorisedController]

  "Unauthorised Controller" must {
    "return 401 for a GET" in {
      val result: Future[Result] = SUT.onPageLoad()(fakeRequest)
      status(result) mustBe UNAUTHORIZED
    }

    "return the correct view for a GET" in {
      val result: Future[Result] = SUT.onPageLoad()(fakeRequest)
      contentAsString(result) mustBe inject[unauthorised].apply(frontendAppConfig)(fakeRequest, messages).toString
    }
  }
}

