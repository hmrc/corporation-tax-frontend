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

package views

import models.CtEnrolment
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.domain.CtUtr
import views.behaviours.ViewBehaviours
import views.html.subpage

class SubpageViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "subpage"

  val ctEnrolment = Some(CtEnrolment(CtUtr("this-is-a-utr"), isActivated = true))

  def createView = () => subpage(frontendAppConfig, ctEnrolment)(HtmlFormat.empty)(fakeRequest, messages)

  "Subpage view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "Subpage sidebar" must {

    "exist" in {
      assertRenderedByTag(asDocument(createView()), "aside")
    }

    "contain the users UTR" in {
      val utrBlock = asDocument(createView()).getElementById("ct-utr")
      utrBlock.text() mustBe "Unique Taxpayer Reference (UTR) this-is-a-utr"
    }
  }
}
