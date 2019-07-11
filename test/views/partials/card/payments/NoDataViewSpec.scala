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

package views.partials.card.payments

import models.requests.AuthenticatedRequest
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import views.ViewSpecBase
import views.html.partials.card.payments.no_data
import models.CtEnrolment
import org.jsoup.nodes.Document
import uk.gov.hmrc.domain.CtUtr

class NoDataViewSpec extends ViewSpecBase {


  def ctEnrolment(activated: Boolean = true) =  CtEnrolment(CtUtr("utr"), isActivated = true)

  def requestWithEnrolment(activated: Boolean): AuthenticatedRequest[AnyContent] = {
    AuthenticatedRequest[AnyContent](FakeRequest(), "", ctEnrolment(activated))
  }
  val fakeRequestWithEnrolments: AuthenticatedRequest[AnyContent] = requestWithEnrolment(activated = true)
  def view = () => no_data(frontendAppConfig)(fakeRequestWithEnrolments, messages)
  lazy val doc: Document = asDocument(view())

  "Partial no_balance view" must {

    "must have text 'You have no tax to pay.' " in {
      doc.getElementsByTag("p").first().text() mustBe "There is no balance information to display."
    }

    "must have 'View your Corporation Tax statement' link" in {
      lazy val viewCtStatement = doc.getElementById("view-ct-statement")
      viewCtStatement.text() mustBe "View your Corporation Tax statement"
      viewCtStatement.attr("href") mustBe "http://localhost:8080/portal/corporation-tax/org/utr/account/balanceperiods?lang=eng"
      viewCtStatement.attr("data-journey-click") mustBe "link - click:CT cards:View your CT statement"
    }

    "must have 'Make a Corporation Tax payment ' link" in {
      lazy val makeCtPayment = doc.getElementById("make-ct-payment")
      makeCtPayment.text() mustBe "Make a Corporation Tax payment"
      makeCtPayment.attr("href") mustBe "http://localhost:9731/business-account/corporation-tax/make-a-payment"
      makeCtPayment.attr("data-journey-click") mustBe "link - click:CT cards:Make a CT payment"
    }
  }
}


