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

package views.partials

import views.ViewSpecBase
import views.html.partials.not_activated

class NotActivatedViewSpec extends ViewSpecBase {

  val activateUrl = "http://activate.url"
  val resetCodeUrl = "http://reset-code.url"

  def viewAfterSevenDays = () => not_activated(activateUrl, resetCodeUrl, true)(fakeRequest, messages)
  def viewWithinSevenDays = () => not_activated(activateUrl, resetCodeUrl, false)(fakeRequest, messages)

  "NotActivated after seven days" should {
    "have a link to activate using the provided url" in {
      val doc = asDocument(viewAfterSevenDays())
      assertLinkById(
        doc,
        "ir-ct-activate",
        "access your Corporation Tax",
        activateUrl
      )
    }

    "have the need activation code content" in {
      asDocument(viewAfterSevenDays()).text() must include("Use the activation code we posted to you to")
      asDocument(viewAfterSevenDays()).text() must include("access your Corporation Tax")
      asDocument(viewAfterSevenDays()).text() must include("It can take up to 72 hours to display your details.")
      asDocument(viewAfterSevenDays()).text() must include("You can")
    }

    "have a link to reset the activation code using the provided url" in {
      assertLinkById(
        asDocument(viewAfterSevenDays()),
        "ir-ct-reset",
        "request a new activation code",
        resetCodeUrl
      )
    }

  }

  "NotActivated within seven days" should {
    "have a link to activate using the provided url" in {
      val doc = asDocument(viewWithinSevenDays())
      assertLinkById(
        doc,
        "ir-ct-activate",
        "access your Corporation Tax",
        activateUrl
      )
    }

    "have the need activation code content" in {
      asDocument(viewWithinSevenDays()).text() must include("We posted an activation code to you. Delivery takes up to 7 days.")
      asDocument(viewWithinSevenDays()).text() must include("Use the activation code to")
      asDocument(viewWithinSevenDays()).text() must include("access your Corporation Tax")
      asDocument(viewWithinSevenDays()).text() must include("It can take up to 72 hours to display your details.")
    }

    "not have a link to reset the activation code using the provided url" in {
      asDocument(viewWithinSevenDays()).getElementById("ir-ct-reset") mustBe null
    }
  }

}
