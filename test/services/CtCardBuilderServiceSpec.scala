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
import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import models._
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import org.joda.time.DateTime
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object TestCtPartialBuilder extends CtPartialBuilder {
  override def buildReturnsPartial()(implicit request: AuthenticatedRequest[_], messages: Messages): Html = Html("Returns partial")

  override def buildPaymentsPartial(accSummaryData: Option[CtData])(implicit request: AuthenticatedRequest[_], messages: Messages): Html = Html("Payments partial")
}

object TestCtPartialBuilderNoData extends CtPartialBuilder {
  override def buildReturnsPartial()(implicit request: AuthenticatedRequest[_], messages: Messages): Html = Html("Returns partial")

  override def buildPaymentsPartial(accSummaryData: Option[CtData])(implicit request: AuthenticatedRequest[_], messages: Messages): Html = Html("There is no balance information to display.")
}

class CtCardBuilderServiceSpec extends SpecBase with ScalaFutures with MockitoSugar {

  class CtCardBuilderServiceTest(messagesApi: MessagesApi,
                                 appConfig: FrontendAppConfig,
                                 ctServiceInterface: CtServiceInterface,
                                 ctPartialBuilder: CtPartialBuilder,
                                 paymentHistoryService: PaymentHistoryServiceInterface
                                ) extends CtCardBuilderServiceImpl(messagesApi, appConfig, ctServiceInterface, ctPartialBuilder, paymentHistoryService)

  case class TestHistoryService(history: List[PaymentRecord]) extends PaymentHistoryServiceInterface {
    override def getPayments(enrolment: CtEnrolment, currentDate: DateTime
                            )(implicit hc: HeaderCarrier): Future[Either[PaymentRecordFailure.type, List[PaymentRecord]]]
    = Future.successful(Right(history))

  }

  trait LocalSetup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val utr: CtUtr = CtUtr("utr")
    lazy val ctEnrolment = CtEnrolment(utr, isActivated = true)
    lazy val testAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
    lazy val testCtService: CtServiceInterface = mock[CtServiceInterface]
    lazy val testCtPartialBuilder: CtPartialBuilder = TestCtPartialBuilder
    lazy val history: List[PaymentRecord] = Nil
    lazy val testHistoryService = TestHistoryService(history)
    lazy val cardBuilderService: CtCardBuilderServiceTest = new CtCardBuilderServiceTest(messagesApi, testAppConfig, testCtService,
      testCtPartialBuilder, testHistoryService)
    lazy val ctData: CtData = CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))

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
      messageReferenceKey = Some("card.ct.utr"),
      paymentsPartial = Some("Payments partial"),
      returnsPartial = Some("Returns partial"),
      paymentHistory = Right(history),
      makePaymentLink = Some(
        Link(
          href = "http://someTestUrl/make-a-payment",
          ga = "link - click:CT cards:Make a CT payment",
          id = "make-ct-payment",
          title = "Make a Corporation Tax payment"
        )
      )
    )

    lazy val testCardNoData: Card = Card(
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
      messageReferenceKey = Some("card.ct.utr"),
      paymentsPartial = Some("There is no balance information to display."),
      returnsPartial = Some("Returns partial"),
      makePaymentLink = Some(
        Link(
          href = "http://someTestUrl/make-a-payment",
          ga = "link - click:CT cards:Make a CT payment",
          id = "make-ct-payment",
          title = "Make a Corporation Tax payment"
        )
      )
    )

    def authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
      AuthenticatedRequest(request = FakeRequest(), externalId = "", ctEnrolment = ctEnrolment)

    when(testAppConfig.getUrl("mainPage")).thenReturn("http://someTestUrl")
    when(testAppConfig.getUrl("fileAReturn")).thenReturn("http://testReturnsUrl")
  }

  "Calling CtCardBuilderService.buildCtCard" should {

    "return a card with Payments information when getting CtData" in new LocalSetup {
      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Right(Some(ctData))))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCard
    }

    "return a card with No Payments information when getting CtNoData" in new LocalSetup {
      override lazy val testCtPartialBuilder: CtPartialBuilder = TestCtPartialBuilderNoData

      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Right(None)))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCardNoData
    }

    "return a card with Returns information when getting CtData" in new LocalSetup {
      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Right(Some(ctData))))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCard
    }

    "return a card with Returns information when getting CtNoData" in new LocalSetup {
      override lazy val testCtPartialBuilder: CtPartialBuilder = TestCtPartialBuilderNoData
      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Right(None)))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)

      result.futureValue mustBe testCardNoData
    }

    "return a card with payment history information when that information is available" in new LocalSetup {
      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Right(Some(ctData))))
      override lazy val history = List(PaymentRecord("test", 100, DateTime.now(), "test-type"))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)
      result.futureValue mustBe testCard
    }

    "throw an exception when getting CtGenericError" in new LocalSetup {
      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Left(CtGenericError)))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)

      result.failed.futureValue mustBe a[Exception]
    }

    "throw an exception when getting CtEmpty" in new LocalSetup {
      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Left(CtEmpty)))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)

      result.failed.futureValue mustBe a[Exception]
    }

    "throw an exception when getting CtUnactivated" in new LocalSetup {
      when(testCtService.fetchCtModel(ctEnrolment)).thenReturn(Future.successful(Left(CtUnactivated)))

      val result: Future[Card] = cardBuilderService.buildCtCard()(authenticatedRequest, hc, messages)

      result.failed.futureValue mustBe a[Exception]
    }

  }

}
