/*
 * Copyright 2023 HM Revenue & Customs
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

package views.partials

import config.FrontendAppConfig
import models.PaymentRecordFailure
import models.payments.PaymentRecord
import play.api.i18n.Messages
import play.api.test.Injecting
import play.twirl.api.Html
import utils.DateUtil
import views.ViewSpecBase
import views.html.partials.{account_summary, payment_history}

import java.time.LocalDateTime

class AccountSummaryViewSpec extends ViewSpecBase with DateUtil with Injecting{

  val testPaymentRecord: PaymentRecord = PaymentRecord(
    reference = "TEST1",
    amountInPence = 100,
    createdOn = LocalDateTime.parse("2018-10-21T08:00:00.000").utcOffset,
    taxType = "tax type"
  )
  val testPaymentHistory
    : Either[PaymentRecordFailure.type, List[PaymentRecord]] = Right(
    List(testPaymentRecord)
  )

  def view: () => Html =
    () =>
      account_summary(
        "hello world",
        frontendAppConfig,
        shouldShowCreditCardMessage = true,
        maybePaymentHistory = testPaymentHistory
      )(fakeRequest, messages)

  "Account summary" when {
    "there is a user" should {
      "display the link to file a return (cato)" in {
        assertLinkById(
          asDocument(view()),
          "ct-file-return-cato",
          "Complete Corporation Tax return",
          "http://localhost:9030/cato",
          expectedRole = "button"
        )
      }

      "display the heading and link to make a payment" in {
        assertLinkById(
          asDocument(view()),
          "ct-make-payment-link",
          "Make a Corporation Tax payment",
          "http://localhost:9731/business-account/corporation-tax/make-a-payment",
          expectedRole = "button"
        )
      }

      "render the provided balance information" in {
        asDocument(view()).getElementsByTag("p").text() must include(
          "hello world"
        )
      }
    }

    "must include the payment_history section" in {
      implicit val implicitMessages: Messages = messages
      implicit val appConfig: FrontendAppConfig = inject[FrontendAppConfig]
      view().toString() must include(
        payment_history(testPaymentHistory, appConfig).toString
      )
    }
  }

}
