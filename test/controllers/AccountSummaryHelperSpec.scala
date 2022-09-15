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

package controllers

import config.FrontendAppConfig
import models._
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContent, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateUtil

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class AccountSummaryHelperSpec extends PlaySpec with MockitoSugar with ScalaFutures
  with BeforeAndAfterEach with GuiceOneAppPerSuite with DateUtil {

  def assertLinkById(doc: Document,
                     linkId: String,
                     expectedText: String,
                     expectedUrl: String,
                     expectedGAEvent: String,
                     expectedIsExternal: Boolean = false,
                     expectedOpensInNewTab: Boolean = false,
                     expectedRole: String = ""): Unit = {
    val link = doc.getElementById(linkId)
    assert(
      link.text() == expectedText,
      s"\n\n Link $linkId does not have text $expectedText"
    )
    assert(
      link.attr("href") == expectedUrl,
      s"\n\n Link $linkId does not expectedUrl $expectedUrl"
    )
    assert(
      link.attr("rel").contains("external") == expectedIsExternal,
      s"\n\n Link $linkId does not meet expectedIsExternal $expectedIsExternal"
    )
    assert(
      link.attr("data-journey-click") == expectedGAEvent,
      s"\n\n Link $linkId does not have expectedGAEvent $expectedGAEvent"
    )
    assert(
      link.attr("target").contains("_blank") == expectedOpensInNewTab,
      s"\n\n Link $linkId does not meet expectedOpensInNewTab $expectedGAEvent"
    )
    assert(
      link.attr("role") == expectedRole,
      s"\n\n Link $linkId does not have role $expectedRole"
    )

  }

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 5 seconds)

  val accountSummary: Html = Html("Account Summary")
  val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  val mockEnrolmentService: EnrolmentStoreService = mock[EnrolmentStoreService]
  val mockCtService: CtService = mock[CtService]
  val mockPaymentHistoryService: PaymentHistoryService = mock[PaymentHistoryService]
  val config: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val messages: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val helper = new AccountSummaryHelper(
    config,
    mockCtService,
    mockEnrolmentService,
    mockPaymentHistoryService,
    messages
  )

  val fakeRequestWithEnrolments: AuthenticatedRequest[AnyContent] = {
    AuthenticatedRequest[AnyContent](FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true))
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    reset(mockCtService)
    reset(mockPaymentHistoryService)
    reset(mockEnrolmentService)
  }

  "getAccountSummaryView" when {
    "rendered" should {
      "show personal credit card message" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(None)))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(false))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include(
            "You can no longer use a personal credit card. If you pay with a credit card, it must be linked to a business bank account.")
        }
      }

      "not show personal credit card message when boolean is false" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(None)))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(false))

        whenReady(helper.getAccountSummaryView(showCreditCardMessage = false)(fakeRequestWithEnrolments, global)) { view =>
          view.toString must not include
            "You can no longer use a personal credit card. If you pay with a credit card, it must be linked to a business bank account."
        }
      }
    }
    "there is no account summary data" should {
      "return 'No Balance information to display'" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(None)))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(false))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include("No balance information to display")
        }
      }
    }
    "there is an error retrieving the data" should {
      "return the generic error message" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Left(CtGenericError)))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(false))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include("We can’t display your Corporation Tax information at the moment.")
        }
      }
    }

    "the user has no CT information available" should {
      "return the generic error message" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Left(CtEmpty)))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(false))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include("We can’t display your Corporation Tax information at the moment.")
        }
      }
    }

    "the user has an unactivated enrolment created more than seven days ago" should {
      "return the not activated view with the new pin link" in {
//        val notActivated = not_activated(
//          "/enrolment-management-frontend/IR-CT/get-access-tax-scheme?continue=%2Fbusiness-account",
//          "/enrolment-management-frontend/IR-CT/request-new-activation-code?continue=%2Fbusiness-account",
//          true
//        )(FakeRequest(), app.injector.instanceOf[Messages])

        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Left(CtUnactivated)))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(true))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include("/enrolment-management-frontend/IR-CT/get-access-tax-scheme?continue=%2Fbusiness-account")
          view.toString must include("/enrolment-management-frontend/IR-CT/request-new-activation-code?continue=%2Fbusiness-account")
        }
      }
    }

    "the user has an unactivated enrolment created less than seven days ago" should {
      "return the not activated view without the new pin link" in {
//        val notActivated = not_activated(
//          "/enrolment-management-frontend/IR-CT/get-access-tax-scheme?continue=%2Fbusiness-account",
//          "/enrolment-management-frontend/IR-CT/request-new-activation-code?continue=%2Fbusiness-account",
//          false
//        )(FakeRequest(), messages)

        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Left(CtUnactivated)))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(false))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include("/enrolment-management-frontend/IR-CT/get-access-tax-scheme?continue=%2Fbusiness-account")
          view.toString must not include "/enrolment-management-frontend/IR-CT/request-new-activation-code?continue=%2Fbusiness-account"
        }
      }
    }

    "the user has a null balance" should {
      "return Nothing to pay" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(None))))))))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(true))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include("You have nothing to pay")
          view.toString must include("View statement")
        }
      }
    }

    "the user has a balance of 0" should {
      "return Nothing to pay" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(0)))))))))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(true))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include("You have nothing to pay")
          view.toString must include("View statement")
        }
      }
    }

    "the user is in credit" should {
      "return You are in credit" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(-123.45)))))))))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(true))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString() must include("You are <span class=\"govuk-!-font-weight-bold\">&pound;123.45</span> in credit")
          view.toString() must include("How we worked this out")
        }
      }
    }

    "the user owes money" should {
      "return You owe money" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(true))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString() must include("You owe <span class=\"govuk-!-font-weight-bold\">&pound;999.99</span>")
          view.toString() must include("How we worked this out")
        }
      }
    }

    "the breakdown link" should {
      "direct the user to the breakdown page" in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))
        when(mockEnrolmentService.showNewPinLink(any(), any())(any()))
          .thenReturn(Future.successful(true))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString() must include("How we worked this out")
          view.toString() must include("You owe <span class=\"govuk-!-font-weight-bold\">&pound;999.99</span>")
          view.toString() must include("How we worked this out")
          view.toString() must include("http://localhost:8081/portal/corporation-tax/org/utr/account/balanceperiods?lang=eng")
          view.toString() must include("""rel="external noopener"""")
        }
      }
    }

    "the user has no payments history" should {
      "not display the payment history section " in {
        when(mockPaymentHistoryService.getPayments(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Nil)))
        when(mockCtService.fetchCtModel(any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString.contains("Your card and bank account payments in the last 7 days") mustBe false
        }
      }
    }

    "the user has a single payment" should {
      "display the appropriate content" in {

        val history = List(
          PaymentRecord(
            reference = "TEST56",
            amountInPence = 1,
            createdOn = LocalDateTime.parse("2018-10-21T08:00:00.000").utcOffset,
            taxType = "tax type"
          )
        )

        when(mockCtService.fetchCtModel(any())(any(),any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))

        when(mockPaymentHistoryService.getPayments(any(),any())(any(), any()))
          .thenReturn(Future.successful(Right(history)))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include ("Your card and bank account payments in the last 7 days")
          view.toString must include ("You paid")
          view.toString must include ("<span class=\"govuk-!-font-weight-bold\">£0.01</span> on 21 October 2018")
          view.toString must include ("Your payment reference number is TEST56.")
        }
      }
    }

    "The user has multiple payments" should {
      "display the appropriate content" in {
        val history = List(
          PaymentRecord(
            reference = "TEST56",
            amountInPence = 1,
            createdOn = LocalDateTime.parse("2018-10-21T08:00:00.000").utcOffset,
            taxType = "tax type"
          ),
          PaymentRecord(
            reference = "TEST56",
            amountInPence = 3,
            createdOn = LocalDateTime.parse("2018-10-22T08:00:00.000").utcOffset,
            taxType = "tax type"
          )
        )

        when(mockCtService.fetchCtModel(any())(any(),any()))
          .thenReturn(Future.successful(Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(999.99)))))))))

        when(mockPaymentHistoryService.getPayments(any(),any())(any(), any()))
          .thenReturn(Future.successful(Right(history)))

        whenReady(helper.getAccountSummaryView()(fakeRequestWithEnrolments, global)) { view =>
          view.toString must include ("Your card and bank account payments in the last 7 days")
          view.toString must include ("You paid")
          view.toString must include ("<span class=\"govuk-!-font-weight-bold\">£0.01</span> on 21 October 2018")
          view.toString must include ("You paid")
          view.toString must include ("<span class=\"govuk-!-font-weight-bold\">£0.03</span> on 22 October 2018")
          view.toString must not include "Your payment reference number is TEST56."
          view.toString must include ("It will take up to 7 days to update your balance after each payment.")
        }
      }
    }
  }

}
