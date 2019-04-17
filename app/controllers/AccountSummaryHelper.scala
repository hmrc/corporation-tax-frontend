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

import javax.inject.Inject

import config.FrontendAppConfig
import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import models.{CtAccountSummary, CtData, CtNoData, CtUnactivated}
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.RequestHeader
import play.twirl.api.HtmlFormat
import services.{CtServiceInterface, EnrolmentsStoreService, PaymentHistoryServiceInterface}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.views.formatting.Money.pounds
import views.html.partials.{account_summary, generic_error, not_activated}

import scala.concurrent.{ExecutionContext, Future}

class AccountSummaryHelper @Inject()(
                                      appConfig: FrontendAppConfig,
                                      ctService: CtServiceInterface,
                                      enrolmentsStoreService: EnrolmentsStoreService,
                                      paymentHistoryService: PaymentHistoryServiceInterface,
                                      override val messagesApi: MessagesApi
                                    ) extends I18nSupport {

  private[controllers] def getAccountSummaryView(showCreditCardMessage: Boolean = true)(implicit r: AuthenticatedRequest[_],
                                                                                        ec:ExecutionContext): Future[HtmlFormat.Appendable] = {

    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers, Some(rh.session))

    val modelHistory: Future[(CtAccountSummary,List[PaymentRecord])] = for{
      model <- ctService.fetchCtModel(Some(r.ctEnrolment))
      history <- paymentHistoryService.getPayments(Some(r.ctEnrolment),DateTime.now())
    } yield {
      (model, history)
    }

    modelHistory flatMap {
      case (CtData(accountSummaryData), history) => Future.successful(
        buildView(accountSummaryData, history, showCreditCardMessage)
      )
      case (CtNoData,_) => Future.successful(
        account_summary(Messages("account.summary.no_balance"), appConfig, shouldShowCreditCardMessage = showCreditCardMessage)
      )
      case (CtUnactivated,_) =>
        val showNewPinLink = enrolmentsStoreService.showNewPinLink(r.ctEnrolment, DateTime.now())
        showNewPinLink.map( showLink =>
            not_activated(appConfig.getUrl("enrolment-management-access"),
              appConfig.getUrl("enrolment-management-new-code"), showLink)
        )
      case _ => Future.successful(generic_error(appConfig.getPortalUrl("home")(r.ctEnrolment)))
    }
  }

  private def buildCtAccountSummaryForKnownBalance(amount: BigDecimal,showCreditCardMessage: Boolean,
                                           breakdownLink: Option[String], history: List[PaymentRecord])(implicit r: AuthenticatedRequest[_]):HtmlFormat.Appendable = {
    if (amount < 0) {
      account_summary(
        Messages("account.summary.in_credit", pounds(amount.abs, 2)),
        appConfig,
        breakdownLink, Messages("account.summary.worked_out"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        history
      )
    } else if (amount == 0) {
      account_summary(
        Messages("account.summary.nothing_to_pay"),
        appConfig,
        breakdownLink, Messages("account.summary.view_statement"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        history
      )
    } else {
      account_summary(
        Messages("account.summary.in_debit", pounds(amount.abs, 2)),
        appConfig,
        breakdownLink, Messages("account.summary.worked_out"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        history
      )
    }
  }

  private def buildView(accountSummaryData: CtAccountSummaryData, history: List[PaymentRecord],
                        showCreditCardMessage: Boolean)(implicit r: AuthenticatedRequest[_]): HtmlFormat.Appendable = {
    val breakdownLink = Some(appConfig.getPortalUrl("balance")(r.ctEnrolment))
    accountSummaryData match {
      case CtAccountSummaryData(Some(CtAccountBalance(Some(amount)))) =>
        buildCtAccountSummaryForKnownBalance(amount, showCreditCardMessage, breakdownLink, history)
      case _ => account_summary(
        Messages("account.summary.nothing_to_pay"),
        appConfig,
        breakdownLink, Messages("account.summary.view_statement"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        history
      )
    }
  }

}
