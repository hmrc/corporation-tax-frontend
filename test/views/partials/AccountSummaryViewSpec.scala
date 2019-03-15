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

package views.partials

import views.ViewSpecBase
import views.html.partials.account_summary

class AccountSummaryViewSpec extends ViewSpecBase {

  def view = () => account_summary("hello world", frontendAppConfig, shouldShowCreditCardMessage = true)(fakeRequest, messages)

  "Account summary" when {
    "there is a user" should {
      "display the link to file a return (cato)" in {
        assertLinkById(asDocument(view()),
          "ct-file-return-cato", "Complete Corporation Tax return", "http://localhost:9030/cato",
          "link - click:CTSubpage:Complete Corporation Tax return")
      }

      "display the heading and link to make a payment" in {
        assertLinkById(asDocument(view()),
          "ct-make-payment-link", "Make a Corporation Tax payment", "http://localhost:9731/business-account/corporation-tax/make-a-payment",
          "link - click:CTSubpage:Make a Corporation Tax payment")
      }

      "render the provided balance information" in {
        asDocument(view()).getElementsByTag("p").text() must include("hello world")
      }
    }
  }
}
