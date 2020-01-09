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

package controllers

import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import models._
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.twirl.api.Html
import services._
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.HeaderCarrier
import views.ViewSpecBase
import views.html.partials.not_activated

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AccountSummaryHelperSpec extends ViewSpecBase with MockitoSugar with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 5 seconds)

  val accountSummary = Html("Account Summary")
  val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  when(mockAccountSummaryHelper.getAccountSummaryView(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(accountSummary))
  val mockEnrolmentService: EnrolmentsStoreService = mock[EnrolmentsStoreService]
  when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))

  val mockCtService: CtService = mock[CtService]
  when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(None)))

  val mockPaymentHistoryService: PaymentHistoryService = mock[PaymentHistoryService]

  def accountSummaryHelper() = new AccountSummaryHelper(frontendAppConfig, mockCtService, mockEnrolmentService,
    mockPaymentHistoryService, messagesApi)

  case class TestCtService(data: Either[CtAccountFailure, Option[CtData]]) extends CtServiceInterface {
    override def fetchCtModel(ctEnrolment: CtEnrolment)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[CtAccountFailure, Option[CtData]]] = {
      Future.successful(data)
    }
  }

  case class TestHistoryService(history: List[PaymentRecord]) extends PaymentHistoryServiceInterface {
    override def getPayments(enrolment: CtEnrolment, date: DateTime)(implicit hc: HeaderCarrier): Future[Either[PaymentRecordFailure.type, List[PaymentRecord]]] = {
      Future.successful(Right(history))
    }
  }

  def ctEnrolment(activated: Boolean = true) = CtEnrolment(CtUtr("utr"), isActivated = true)

  def requestWithEnrolment(activated: Boolean): AuthenticatedRequest[AnyContent] = {
    AuthenticatedRequest[AnyContent](FakeRequest(), "", ctEnrolment(activated))
  }

  val fakeRequestWithEnrolments: AuthenticatedRequest[AnyContent] = requestWithEnrolment(activated = true)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getAccountSummaryView" when {
    "rendered" should {
      "show personal credit card message" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(None)))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          view.toString must include(
            "You can no longer use a personal credit card. If you pay with a credit card, it must be linked to a business bank account.")
        }
      }

      "not show personal credit card message when boolean is false" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(None)))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
        whenReady(accountSummaryHelper().getAccountSummaryView(showCreditCardMessage = false)
        (fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          view.toString must not include
            "You can no longer use a personal credit card. If you pay with a credit card, it must be linked to a business bank account."
        }
      }
    }
    "there is no account summary data" should {
      "return 'No Balance information to display'" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(None)))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          view.toString must include("No balance information to display")
        }
      }
    }
    "there is an error retrieving the data" should {
      "return the generic error message" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Left(CtGenericError)))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          view.toString must include("We can’t display your Corporation Tax information at the moment.")
        }
      }
    }

    "the user has no CT information available" should {
      "return the generic error message" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Left(CtEmpty)))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          view.toString must include("We can’t display your Corporation Tax information at the moment.")
        }
      }
    }

    "the user has an unactivated enrolment created more than seven days ago" should {
      "return the not activated view with the new pin link" in {
        val notActivated = not_activated(
          "/enrolment-management-frontend/IR-CT/get-access-tax-scheme?continue=%2Fbusiness-account",
          "/enrolment-management-frontend/IR-CT/request-new-activation-code?continue=%2Fbusiness-account",
          true
        )(fakeRequest, messages)

        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Left(CtUnactivated)))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          view mustBe notActivated
        }
      }
    }

    "the user has an unactivated enrolment created less than seven days ago" should {
      "return the not activated view without the new pin link" in {
        val notActivated = not_activated(
          "/enrolment-management-frontend/IR-CT/get-access-tax-scheme?continue=%2Fbusiness-account",
          "/enrolment-management-frontend/IR-CT/request-new-activation-code?continue=%2Fbusiness-account",
          false
        )(fakeRequest, messages)

        reset(mockCtService)
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Left(CtUnactivated)))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(false))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          view mustBe notActivated
        }
      }
    }

    "the user has a null balance" should {
      "return Nothing to pay" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(None))))))))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          asDocument(view).text() must include("You have nothing to pay View statement (opens in a new window or tab)")
        }
      }
    }

    "the user has a balance of 0" should {
      "return Nothing to pay" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0)))))))))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          asDocument(view).text() must include("You have nothing to pay View statement (opens in a new window or tab)")
        }
      }
    }

    "the user is in credit" should {
      "return You are in credit" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(-123.45)))))))))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          asDocument(view).text() must include("You are £123.45 in credit How we worked this out")
        }
      }
    }

    "the user owes money" should {
      "return You owe money" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          asDocument(view).text() must include("You owe £999.99 How we worked this out")
        }
      }
    }

    "the breakdown link" should {
      "direct the user to the breakdown page" in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))
        reset(mockEnrolmentService)
        when(mockEnrolmentService.showNewPinLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(true))
        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          assertLinkById(asDocument(view), "ct-see-breakdown", "How we worked this out (opens in a new window or tab)",
            "http://localhost:8080/portal/corporation-tax/org/utr/account/balanceperiods?lang=eng",
            "link - click:CTSubpage:How we worked this out", expectedIsExternal = true, expectedOpensInNewTab = true)
        }
      }
    }

    "the user has no payments history" should {
      "not display the payment history section " in {
        reset(mockCtService)
        reset(mockPaymentHistoryService)
        when(mockPaymentHistoryService.getPayments(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))

        whenReady(accountSummaryHelper().getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          asDocument(view).html.contains("Your card payments in the last 7 days") mustBe false
        }
      }
    }

    "the user has a single payment" should {
      "display the appropriate content" in {

        val history = List(
          PaymentRecord(
            reference = "TEST56",
            amountInPence = 100,
            createdOn = new DateTime("2018-10-21T08:00:00.000"),
            taxType = "tax type"
          )
        )

        val ctService = TestCtService(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99))))))))
        val historyService = TestHistoryService(history)

        val testAccountSummaryHelper = new AccountSummaryHelper(frontendAppConfig, ctService, mockEnrolmentService, historyService, messagesApi)

        whenReady(testAccountSummaryHelper.getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          asDocument(view).html.contains("Your card payments in the last 7 days") mustBe true
          asDocument(view).html.contains("You paid £1 on 21 October 2018") mustBe true
          asDocument(view).html.contains("Your payment reference number is TEST56.") mustBe true
        }
      }
    }

    "The user has multiple payments" should {
      "display the appropriate content" in {
        val history = List(
          PaymentRecord(
            reference = "TEST56",
            amountInPence = 100,
            createdOn = new DateTime("2018-10-21T08:00:00.000"),
            taxType = "tax type"
          ),
          PaymentRecord(
            reference = "TEST56",
            amountInPence = 300,
            createdOn = new DateTime("2018-10-22T08:00:00.000"),
            taxType = "tax type"
          )
        )

        val ctService = TestCtService(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99))))))))
        val historyService = TestHistoryService(history)

        val testAccountSummaryHelper = new AccountSummaryHelper(frontendAppConfig, ctService, mockEnrolmentService, historyService, messagesApi)

        whenReady(testAccountSummaryHelper.getAccountSummaryView()(fakeRequestWithEnrolments, scala.concurrent.ExecutionContext.global)) { view =>
          asDocument(view).html.contains("Your card payments in the last 7 days") mustBe true
          asDocument(view).html.contains("You paid £1 on 21 October 2018") mustBe true
          asDocument(view).html.contains("You paid £3 on 22 October 2018") mustBe true
          asDocument(view).html.contains("Your payment reference number is TEST56.") mustBe false
          asDocument(view).html.contains("It will take up to 7 days to update your balance after each payment.") mustBe true
        }
      }
    }
  }

}
