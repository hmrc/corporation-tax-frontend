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

package views
import views.html.subpage
import models.CtEnrolment
import play.twirl.api.{Html, HtmlFormat}
import models.CtUtr
import play.api.test.Injecting
import views.behaviours.ViewBehaviours

class SubpageViewSpec extends ViewBehaviours with Injecting {

  val messageKeyPrefix = "subpage"

  val utr = CtUtr("this-is-a-utr")
  val ctEnrolment = CtEnrolment(utr, isActivated = true)

  val partialContent = Html("<p id=\"partial-content\">hello world</p>")
  def createView(): Html = inject[subpage].apply(frontendAppConfig, ctEnrolment, partialContent)(HtmlFormat.empty)(fakeRequest, messages)

  "Subpage view" must {
    behave like normalPage(createView, messageKeyPrefix)

    "contain heading ID" in {
      val doc = asDocument(createView())
      doc.getElementsByTag("h1").attr("id") mustBe "ct-details"
    }

    "contain correct content" in {
      val doc = asDocument(createView())
      doc.getElementsByTag("h1").first().text() mustBe "Corporation Tax details"

      val paragraph = doc.getElementById("payments-notice").text

      paragraph mustBe Seq(
        "Payments will take up to 7 working days to show, depending on how you pay.",
        "After you complete your return your tax calculation will take up to 2 days."
      ).mkString(" ")
    }

    "render the provided partial content" in {
      val doc = asDocument(createView())
        .getElementById("partial-content")
        .text mustBe "hello world"
    }
  }

  "Subpage sidebar" must {

    "exist" in {
      assertRenderedByTag(asDocument(createView()), "aside")
    }

    "contain the users UTR" in {
      val utrBlock = asDocument(createView()).getElementById("ct-utr")
      utrBlock
        .text() mustBe "Corporation Tax Unique Taxpayer Reference (UTR): this-is-a-utr"
    }

    "have aria-label" in {
      val doc = asDocument(createView())
      doc
        .getElementById("ct-sub-navigation")
        .attr("aria-label") mustBe "Corporation Tax sub navigation"
    }

     "contain the more options links" in {
      val doc = asDocument(createView())
      doc
        .getElementById("more-options")
        .getElementsByTag("h3")
        .text() mustBe "More options"
      assertLinkById(
        doc,
        "cert-of-residence",
        "Get a certificate of residence (opens in a new tab)",
        "https://www.gov.uk/guidance/get-a-certificate-of-residence",
        expectedGAEvent =
          "link - click:CTMoreOptions:Get a certificate of residence",
        expectedIsExternal = true,
        expectedOpensInNewTab = true
      )
      assertLinkById(
        doc,
        "help-and-contact",
        "Help and contact",
        "http://localhost:9733/business-account/help",
        expectedGAEvent = "link - click:CTSidebar:Help and contact"
      )
      assertLinkById(
        doc,
        "more",
        "More (opens in a new tab)",
        s"http://localhost:8080/portal/corporation-tax/org/$utr/account/balanceperiods?lang=eng",
        expectedGAEvent = "link - click:CTSidebar:More",
        expectedIsExternal = true,
        expectedOpensInNewTab = true
      )
    }
  }
}
