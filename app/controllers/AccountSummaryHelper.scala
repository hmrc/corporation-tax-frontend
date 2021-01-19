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

package controllers

import config.FrontendAppConfig
import javax.inject.Inject
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import models.{CtAccountBalance, CtAccountFailure, CtAccountSummaryData, CtData, CtUnactivated, PaymentRecordFailure}
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.MessagesControllerComponents
import play.twirl.api.HtmlFormat
import services.{CtService, EnrolmentStoreService, PaymentHistoryService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Money.pounds
import views.html.partials.{account_summary, generic_error, not_activated}

import scala.concurrent.{ExecutionContext, Future}

class AccountSummaryHelper @Inject()(appConfig: FrontendAppConfig,
                                     ctService: CtService,
                                     enrolmentStoreService: EnrolmentStoreService,
                                     paymentHistoryService: PaymentHistoryService,
                                     mcc: MessagesControllerComponents) extends FrontendController(mcc) with I18nSupport {

  private[controllers] def getAccountSummaryView(showCreditCardMessage: Boolean = true)(implicit request: AuthenticatedRequest[_],
                                                                                        ec: ExecutionContext): Future[HtmlFormat.Appendable] = {
    val modelHistory: Future[(Either[CtAccountFailure, Option[CtData]], Either[PaymentRecordFailure.type, List[PaymentRecord]])] =
      for {
        model <- ctService.fetchCtModel(request.ctEnrolment)
        maybeHistory <- paymentHistoryService.getPayments(request.ctEnrolment, DateTime.now())
      } yield (model, maybeHistory)

    modelHistory flatMap {
      case (Right(Some(CtData(accountSummaryData))), maybeHistory) =>
        Future.successful(buildView(accountSummaryData, maybeHistory, showCreditCardMessage))
      case (Right(None), _) =>
        Future.successful(account_summary(Messages("account.summary.no_balance"), appConfig, shouldShowCreditCardMessage = showCreditCardMessage))
      case (Left(CtUnactivated), _) =>
        enrolmentStoreService
          .showNewPinLink(request.ctEnrolment, DateTime.now())
          .map(showLink => not_activated(
            activateUrl = appConfig.getUrl("enrolment-management-access"),
            resetCodeUrl = appConfig.getUrl("enrolment-management-new-code"),
            showNewPinLink = showLink
          ))
      case _ =>
        Future.successful(generic_error(appConfig.getPortalUrl("home")(request.ctEnrolment)))
    }
  }

  private def buildCtAccountSummaryForKnownBalance(amount: BigDecimal,
                                                   showCreditCardMessage: Boolean,
                                                   breakdownLink: Option[String],
                                                   maybeHistory: Either[PaymentRecordFailure.type, List[PaymentRecord]])
                                                  (implicit r: AuthenticatedRequest[_]): HtmlFormat.Appendable = {
    if (amount < 0) {
      account_summary(
        Messages("account.summary.in_credit", pounds(amount.abs, 2)),
        appConfig,
        breakdownLink, Messages("account.summary.worked_out"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        maybeHistory
      )
    } else if (amount == 0) {
      account_summary(
        Messages("account.summary.nothing_to_pay"),
        appConfig,
        breakdownLink, Messages("account.summary.view_statement"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        maybeHistory
      )
    } else {
      account_summary(
        Messages("account.summary.in_debit", pounds(amount.abs, 2)),
        appConfig,
        breakdownLink, Messages("account.summary.worked_out"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        maybeHistory
      )
    }
  }

  private def buildView(accountSummaryData: CtAccountSummaryData, maybeHistory: Either[PaymentRecordFailure.type, List[PaymentRecord]],
                        showCreditCardMessage: Boolean)(implicit r: AuthenticatedRequest[_]): HtmlFormat.Appendable = {
    val breakdownLink = Some(appConfig.getPortalUrl("balance")(r.ctEnrolment))
    accountSummaryData match {
      case CtAccountSummaryData(Some(CtAccountBalance(Some(amount)))) =>
        buildCtAccountSummaryForKnownBalance(amount, showCreditCardMessage, breakdownLink, maybeHistory)
      case _ => account_summary(
        Messages("account.summary.nothing_to_pay"),
        appConfig,
        breakdownLink, Messages("account.summary.view_statement"),
        shouldShowCreditCardMessage = showCreditCardMessage,
        maybeHistory
      )
    }
  }

}
