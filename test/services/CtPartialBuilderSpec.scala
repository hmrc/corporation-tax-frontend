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

package services

import config.FrontendAppConfig
import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import models.requests.AuthenticatedRequest
import models.{Card, CtData, CtEnrolment, Link}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.CtUtr
import views.ViewSpecBase

import scala.concurrent.ExecutionContext.Implicits.global


class CtPartialBuilderSpec extends ViewSpecBase with OneAppPerSuite with MockitoSugar with ScalaFutures with MustMatchers {

  trait LocalSetup {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val fakeRequestWithEnrolments: AuthenticatedRequest[AnyContent] = requestWithEnrolment(activated = true)

    lazy val utr: CtUtr = CtUtr("utr")
    lazy val ctEnrolment = CtEnrolment(utr, isActivated = true)
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
      paymentsPartial = Some("Work in Progress"),
      returnsPartial = Some("\n\n<p>You may have returns to complete.</p>\n<a id=\"ct-complete-return\" href=\"http://testReturnsUrl\"\n  target=\"_blank\" rel=\"external noopener\"\n  data-journey-click=\"link - click:CT cards:Complete CT Return\">\nComplete Corporation Tax return\n</a>\n")
    )

    def requestWithEnrolment(activated: Boolean): AuthenticatedRequest[AnyContent] = {
      AuthenticatedRequest[AnyContent](FakeRequest(), "", ctEnrolment)
    }

    when(config.getUrl("fileAReturn")).thenReturn("http://localhost:9030/cato")
  }

  "Calling CtPartialBuilder.buildReturnsPartial" should {
    "handle returns" in new LocalSetup {
      val ctPartialBuilder: CtPartialBuilderImpl = new CtPartialBuilderImpl(config)
      val view: String =  ctPartialBuilder.buildReturnsPartial()(fakeRequestWithEnrolments, messages).body
      val doc: Document = Jsoup.parse(view)

      doc.text() must include("You may have returns to complete.")

      assertLinkById(doc,
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
    "handle payments" in new LocalSetup {
      val ctPartialBuilder: CtPartialBuilderImpl = new CtPartialBuilderImpl(config)
      val view: String =  ctPartialBuilder.buildPaymentsPartial(Some(ctData))(fakeRequestWithEnrolments, messages).body
      val doc: Document = Jsoup.parse(view)

      doc.text() must include("Work in Progress")
    }
  }

}