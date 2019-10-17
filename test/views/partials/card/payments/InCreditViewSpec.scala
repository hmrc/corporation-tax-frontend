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

import models.CtEnrolment
import models.requests.AuthenticatedRequest
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.CtUtr
import views.ViewSpecBase
import views.html.partials.card.payments.in_credit

class InCreditViewSpec extends ViewSpecBase {
  def ctEnrolment(activated: Boolean = true) =  CtEnrolment(CtUtr("utr"), isActivated = true)

  def requestWithEnrolment(activated: Boolean): AuthenticatedRequest[AnyContent] = {
    AuthenticatedRequest[AnyContent](FakeRequest(), "", ctEnrolment(activated))
  }

  lazy val fakeRequestWithEnrolments: AuthenticatedRequest[AnyContent] = requestWithEnrolment(activated = true)
  lazy val inCreditAmount: BigDecimal = BigDecimal(123.45)
  def view = () => in_credit(inCreditAmount, frontendAppConfig)(fakeRequestWithEnrolments, messages)

  lazy val doc: Document = asDocument(view())

  "Partial in_credit view" must {

    "must have text 'You are £123.45 in credit.' " in {
      doc.getElementsByTag("p").first().text() mustBe "You are £123.45 in credit."
    }
  }
  
}
