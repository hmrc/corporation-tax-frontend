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

package controllers

import config.FrontendAppConfig
import models._
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.MessagesControllerComponents
import play.twirl.api.HtmlFormat
import services.{CtService, EnrolmentStoreService, PaymentHistoryService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{LoggingUtil, MoneyPounds}
import views.html.partials.{account_summary, generic_error, not_activated}

import java.time.OffsetDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccountSummaryHelper @Inject()(appConfig: FrontendAppConfig,
                                     ctService: CtService,
                                     enrolmentStoreService: EnrolmentStoreService,
                                     paymentHistoryService: PaymentHistoryService,
                                     mcc: MessagesControllerComponents) extends FrontendController(mcc) with I18nSupport with LoggingUtil{

  private[controllers] def getAccountSummaryView(showCreditCardMessage: Boolean = true)(implicit request: AuthenticatedRequest[_],
                                                                                        ec: ExecutionContext): Future[HtmlFormat.Appendable] = {
    infoLog(s"[AccountSummaryHelper][getAccountSummaryView] - Attempted to get AccountSummaryView")
    val modelHistory: Future[(Either[CtAccountFailure, Option[CtData]], Either[PaymentRecordFailure.type, List[PaymentRecord]])] =
      for {
        model <- ctService.fetchCtModel(request.ctEnrolment)
        maybeHistory <- paymentHistoryService.getPayments(request.ctEnrolment, OffsetDateTime.now())
      } yield (model, maybeHistory)

    modelHistory flatMap {
      case (Right(Some(CtData(accountSummaryData))), maybeHistory) =>
        infoLog(s"[AccountSummaryHelper][getAccountSummaryView] - Succeeded")
        Future.successful(buildView(accountSummaryData, maybeHistory, showCreditCardMessage))
      case (Right(None), _) =>
        infoLog(s"[AccountSummaryHelper][getAccountSummaryView] - No balance to show")
        Future.successful(account_summary(Messages("account.summary.no_balance"), appConfig, shouldShowCreditCardMessage = showCreditCardMessage))
      case (Left(CtUnactivated), _) =>
        warnLog(s"[AccountSummaryHelper][getAccountSummaryView] - Unactivated enrolment")
        enrolmentStoreService
          .showNewPinLink(request.ctEnrolment, OffsetDateTime.now())
          .map(showLink => not_activated(
            activateUrl = appConfig.getUrl("enrolment-management-access"),
            resetCodeUrl = appConfig.getUrl("enrolment-management-new-code"),
            showNewPinLink = showLink
          ))
      case _ =>
        errorLog(s"[AccountSummaryHelper][getAccountSummaryView] - Failed to show account summary view")
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
        Messages("account.summary.in_credit", MoneyPounds.pounds(amount.abs, 2)),
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
        Messages("account.summary.in_debit", MoneyPounds.pounds(amount.abs, 2)),
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
      case CtAccountSummaryData(CtAccountBalance(amount), _) =>
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
