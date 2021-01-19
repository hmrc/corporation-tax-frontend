/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import config.FrontendAppConfig
import models.requests.AuthenticatedRequest
import models.{Card, CtAccountBalance, CtAccountSummaryData, CtData, CtEnrolment, CtUtr, Link}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import views.ViewSpecBase


class CtPartialBuilderSpec extends ViewSpecBase with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures with MustMatchers {

  trait LocalSetup {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val fakeRequestWithEnrolments: AuthenticatedRequest[AnyContent] = requestWithEnrolment(activated = true)

    lazy val utr: CtUtr = CtUtr("utr")
    lazy val ctEnrolment: CtEnrolment = CtEnrolment(utr, isActivated = true)
    lazy val config: FrontendAppConfig = mock[FrontendAppConfig]
    lazy val ctData: CtData = CtData(CtAccountSummaryData(Some(CtAccountBalance(None))))

    lazy val testCard: Card = Card(
      title = "Corporation Tax",
      description = "",
      referenceNumber = "utr",
      primaryLink = Some(
        Link(
          id = "ct-account-details-card-link",
          title = "Corporation Tax",
          href = "http://someTestUrl",
          ga = "link - click:CT cards:More CT details",
          dataSso = None,
          external = false
        )
      ),
      messageReferenceKey = Some(""),
      paymentsPartial = Some("Payments partial"),
      returnsPartial = Some("\n\n<p>You may have returns to complete.</p>\n<a id=\"ct-complete-return\" href=\"http://testReturnsUrl\"\n  target=\"_blank\" rel=\"external noopener\"\n  data-journey-click=\"link - click:CT cards:Complete CT Return\">\nComplete Corporation Tax return\n</a>\n")
    )

    def requestWithEnrolment(activated: Boolean): AuthenticatedRequest[AnyContent] = {
      AuthenticatedRequest[AnyContent](FakeRequest(), "", ctEnrolment)
    }

    when(config.getUrl("fileAReturn")).thenReturn("http://localhost:9030/cato")
    when(config.getUrl("mainPage")).thenReturn("http://localhost:9731/business-account/corporation-tax")
    when(config.getPortalUrl("balance")(ctEnrolment)(fakeRequestWithEnrolments))
        .thenReturn("http://localhost:8080/portal/corporation-tax/org/utr/account/balanceperiods?lang=eng")
  }

  "Calling CtPartialBuilder.buildReturnsPartial" should {

    "handle returns" in new LocalSetup {
      val ctPartialBuilder: CtPartialBuilder = new CtPartialBuilder(config)
      val view: String =  ctPartialBuilder.buildReturnsPartial()(fakeRequestWithEnrolments, messages).body
      val doc: Document = Jsoup.parse(view)

      doc.text() must include("You may have returns to complete.")

      assertLinkById(
        doc,
        linkId = "ct-complete-return",
        expectedText = "Complete Corporation Tax return",
        expectedUrl = "http://localhost:9030/cato",
        expectedGAEvent = "link - click:CT cards:Complete CT Return",
        expectedIsExternal = true,
        expectedOpensInNewTab = true
      )
    }

  }

  "Calling CtPartialBuilder.buildPaymentsPartial" should {

    "handle payments" when {

      "the user is in credit with nothing to pay" in new LocalSetup {
        val ctPartialBuilder: CtPartialBuilder = new CtPartialBuilder(config)
        override lazy val ctData: CtData = CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(-123.45)))))
        val view: String =  ctPartialBuilder.buildPaymentsPartial(Some(ctData))(fakeRequestWithEnrolments, messages).body
        val doc: Document = Jsoup.parse(view)

        doc.text() must include("You are £123.45 in credit.")
      }

      "the user is in debit" in new LocalSetup {
        val ctPartialBuilder: CtPartialBuilder = new CtPartialBuilder(config)
        override lazy val ctData: CtData = CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(543.21)))))
        val view: String =  ctPartialBuilder.buildPaymentsPartial(Some(ctData))(fakeRequestWithEnrolments, messages).body
        val doc: Document = Jsoup.parse(view)

        doc.text() must include("You owe £543.21.")
      }

      "the user has no tax to pay" in new LocalSetup {
        val ctPartialBuilder: CtPartialBuilder = new CtPartialBuilder(config)
        override lazy val ctData: CtData = CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0)))))
        val view: String =  ctPartialBuilder.buildPaymentsPartial(Some(ctData))(fakeRequestWithEnrolments, messages).body
        val doc: Document = Jsoup.parse(view)

        doc.text() must include("You have no tax to pay.")

      }

      "there is no balance information to display" in new LocalSetup {
        val ctPartialBuilder: CtPartialBuilder = new CtPartialBuilder(config)
        override lazy val ctData: CtData = CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0)))))
        val view: String =  ctPartialBuilder.buildPaymentsPartial(None)(fakeRequestWithEnrolments, messages).body
        val doc: Document = Jsoup.parse(view)

        doc.text() must include("There is no balance information to display.")
      }

    }

  }

}
