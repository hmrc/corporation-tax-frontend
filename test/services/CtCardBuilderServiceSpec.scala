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

import base.SpecBase
import config.FrontendAppConfig
import models.requests.AuthenticatedRequest
import models.{Card, CtEnrolment, CtNoData, Link}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{CtUtr, Vrn}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CtCardBuilderServiceSpec extends SpecBase with ScalaFutures with MockitoSugar {

  class CtCardBuilderServiceTest(messagesApi: MessagesApi,
                                 appConfig: FrontendAppConfig,
                                 ctServiceInterface: CtServiceInterface
                                ) extends CtCardBuilderServiceImpl(messagesApi, appConfig, ctServiceInterface)

  trait LocalSetup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val utr: CtUtr = CtUtr("utr")
    lazy val ctEnrolment = CtEnrolment(utr, isActivated = true)
    lazy val testAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
    lazy val testCtService: CtServiceInterface = mock[CtServiceInterface]
    lazy val service: CtCardBuilderServiceTest = new CtCardBuilderServiceTest(messagesApi, testAppConfig, testCtService)

    def authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
      AuthenticatedRequest(request = FakeRequest(), externalId = "", ctEnrolment = ctEnrolment)

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

    when(testAppConfig.getUrl("mainPage")).thenReturn("http://someTestUrl")
    when(testAppConfig.getUrl("fileAReturn")).thenReturn("http://testReturnsUrl")
  }


  "Calling CtCardBuilderService.buildCtCard" should {

    "return a card with Returns information" in new LocalSetup {
      when(testCtService.fetchCtModel(Some(ctEnrolment))).thenReturn(Future.successful(CtNoData))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCard
    }

  }

}
