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

package controllers

import config.FrontendAppConfig
import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import javax.inject.Inject

import models.requests.AuthenticatedRequest
import models.{CtData, CtNoData, CtUnactivated}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.RequestHeader
import play.twirl.api.HtmlFormat
import services.CtService
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.views.formatting.Money.pounds
import views.html.partials.{account_summary, generic_error, not_activated}

import scala.concurrent.ExecutionContext

class AccountSummaryHelper @Inject()(
                                      appConfig: FrontendAppConfig,
                                      ctService: CtService,
                                      override val messagesApi: MessagesApi
                                    ) extends I18nSupport {

  private[controllers] def getAccountSummaryView(showCreditCardMessage: Boolean = true)(implicit r: AuthenticatedRequest[_], ec:ExecutionContext) = {

    implicit def hc(implicit rh: RequestHeader) = HeaderCarrierConverter.fromHeadersAndSession(rh.headers, Some(rh.session))

    val breakdownLink = Some(appConfig.getPortalUrl("balance")(r.ctEnrolment))

    ctService.fetchCtModel(Some(r.ctEnrolment)) map {
      case CtData(accountSummaryData) => accountSummaryData match {
        case CtAccountSummaryData(Some(CtAccountBalance(Some(amount)))) =>
          if (amount < 0) {
            account_summary(
              Messages("account.summary.in_credit", pounds(amount.abs, 2)),
              appConfig,
              breakdownLink, Messages("account.summary.worked_out"),
              shouldShowCreditCardMessage = showCreditCardMessage
            )
          } else if (amount == 0) {
            account_summary(
              Messages("account.summary.nothing_to_pay"),
              appConfig,
              breakdownLink, Messages("account.summary.view_statement"),
              shouldShowCreditCardMessage = showCreditCardMessage
            )
          } else {
            account_summary(
              Messages("account.summary.in_debit", pounds(amount.abs, 2)),
              appConfig,
              breakdownLink, Messages("account.summary.worked_out"),
              shouldShowCreditCardMessage = showCreditCardMessage
            )
          }
        case _ => account_summary(
          Messages("account.summary.nothing_to_pay"),
          appConfig,
          breakdownLink, Messages("account.summary.view_statement"),
          shouldShowCreditCardMessage = showCreditCardMessage
        )
      }
      case CtNoData => account_summary(Messages("account.summary.no_balance"), appConfig, shouldShowCreditCardMessage = showCreditCardMessage)
      case CtUnactivated => not_activated(appConfig.getPortalUrl("activate")(r.ctEnrolment), appConfig.getPortalUrl("reset")(r.ctEnrolment))
      case _ => generic_error(appConfig.getPortalUrl("home")(r.ctEnrolment))
    }
  }
}
