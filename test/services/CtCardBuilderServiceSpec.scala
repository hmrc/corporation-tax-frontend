/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.twirl.api.Html
import models.{Card, CtAccountBalance, CtAccountSummaryData, CtData, CtEmpty, CtEnrolment, CtGenericError, CtUnactivated, CtUtr, Link}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CtCardBuilderServiceSpec extends SpecBase with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val ctEnrolment: CtEnrolment = CtEnrolment(CtUtr("utr"), isActivated = true)

  val ctData: CtData = CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))
  val testCard: Card = Card(
    title = "Corporation Tax",
    description = "",
    referenceNumber = "utr",
    primaryLink = Some(
      Link(
        id = "ct-account-details-card-link",
        title = "Corporation Tax",
        href = "http://someTestUrl",
        ga = "link - click:CT cards:More CT details",
        dataSso = None
      )
    ),
    messageReferenceKey = Some("card.ct.utr"),
    paymentsPartial = Some("Payments partial"),
    returnsPartial = Some("Returns partial"),
    paymentHistory = Right(Nil),
    paymentSectionAdditionalLinks = Some(
      List(
        Link(
          href = "http://testStatementUrl",
          ga = "link - click:CT cards:View your CT statement",
          id = "view-ct-statement",
          title = "View your Corporation Tax statement"
        ),
        Link(
          href = "http://someTestUrl/make-a-payment",
          ga = "link - click:CT cards:Make a CT payment",
          id = "make-ct-payment",
          title = "Make a Corporation Tax payment"
        )
      )
    ),
    accountBalance = Some(999.99)
  )

  val testCardNoData: Card = Card(
    title = "Corporation Tax",
    description = "",
    referenceNumber = "utr",
    primaryLink = Some(
      Link(
        id = "ct-account-details-card-link",
        title = "Corporation Tax",
        href = "http://someTestUrl",
        ga = "link - click:CT cards:More CT details",
        dataSso = None
      )
    ),
    messageReferenceKey = Some("card.ct.utr"),
    paymentsPartial = Some("There is no balance information to display."),
    returnsPartial = Some("Returns partial"),
    paymentSectionAdditionalLinks = Some(
      List(
        Link(
          href = "http://testStatementUrl",
          ga = "link - click:CT cards:View your CT statement",
          id = "view-ct-statement",
          title = "View your Corporation Tax statement"
        ),
        Link(
          href = "http://someTestUrl/make-a-payment",
          ga = "link - click:CT cards:Make a CT payment",
          id = "make-ct-payment",
          title = "Make a Corporation Tax payment"
        )
      )
    ),
    accountBalance = None
  )

  def authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(request = FakeRequest(), externalId = "", ctEnrolment = ctEnrolment)

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockCtService: CtService = mock[CtService]
  val mockPartialBuilder: CtPartialBuilder = mock[CtPartialBuilder]
  val mockHistoryService: PaymentHistoryService = mock[PaymentHistoryService]

  val service: CtCardBuilderService = new CtCardBuilderService(messagesApi, mockAppConfig, mockCtService, mockPartialBuilder, mockHistoryService)

  override def beforeEach(): Unit = {
    when(mockAppConfig.getUrl(any()))
      .thenReturn("http://someTestUrl")
    when(mockAppConfig.getPortalUrl(any())(any())(any()))
      .thenReturn("http://testStatementUrl")
  }

  "Calling CtCardBuilderService.buildCtCard" should {
    "return a card with Payments information when getting CtData" in {
      when(mockCtService.fetchCtModel(any())(any(),any()))
        .thenReturn(Future.successful(Right(Some(ctData))))
      when(mockHistoryService.getPayments(any(), any())(any()))
        .thenReturn(Future.successful(Right(Nil)))

      when(mockPartialBuilder.buildPaymentsPartial(any())(any(),any()))
        .thenReturn(Html("Payments partial"))
      when(mockPartialBuilder.buildReturnsPartial()(any(),any()))
        .thenReturn(Html("Returns partial"))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCard
    }

    "return a card with No Payments information when getting CtNoData" in {
      when(mockCtService.fetchCtModel(ctEnrolment))
        .thenReturn(Future.successful(Right(None)))

      when(mockPartialBuilder.buildPaymentsPartial(any())(any(),any()))
        .thenReturn(Html("There is no balance information to display."))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCardNoData
    }

    "return a card with Returns information when getting CtData" in {
      when(mockCtService.fetchCtModel(ctEnrolment))
        .thenReturn(Future.successful(Right(Some(ctData))))

      when(mockPartialBuilder.buildPaymentsPartial(any())(any(),any()))
        .thenReturn(Html("Payments partial"))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCard
    }

    "return a card with Returns information when getting CtNoData" in {
      when(mockCtService.fetchCtModel(ctEnrolment))
        .thenReturn(Future.successful(Right(None)))

      when(mockPartialBuilder.buildPaymentsPartial(any())(any(),any()))
        .thenReturn(Html("There is no balance information to display."))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCardNoData
    }

    "return a card with payment history information when that information is available" in {
      when(mockCtService.fetchCtModel(ctEnrolment))
        .thenReturn(Future.successful(Right(Some(ctData))))

      when(mockPartialBuilder.buildPaymentsPartial(any())(any(),any()))
        .thenReturn(Html("Payments partial"))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)
      result.futureValue mustBe testCard
    }

    "throw an exception when getting CtGenericError" in {
      when(mockCtService.fetchCtModel(ctEnrolment))
        .thenReturn(Future.successful(Left(CtGenericError)))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.failed.futureValue mustBe a[Exception]
    }

    "throw an exception when getting CtEmpty" in {
      when(mockCtService.fetchCtModel(ctEnrolment))
        .thenReturn(Future.successful(Left(CtEmpty)))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.failed.futureValue mustBe a[Exception]
    }

    "throw an exception when getting CtUnactivated" in {
      when(mockCtService.fetchCtModel(ctEnrolment))
        .thenReturn(Future.successful(Left(CtUnactivated)))

      val result: Future[Card] = service.buildCtCard()(authenticatedRequest, hc, messages)

      result.failed.futureValue mustBe a[Exception]
    }

  }

}
