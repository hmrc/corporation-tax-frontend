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

import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import javax.inject.{Inject, Singleton}
import models.requests.AuthenticatedRequest
import models.{CtAccountSummary, CtData}
import javax.inject.{Inject, Singleton}
import models.{CtData, CtEnrolment}
import models.requests.AuthenticatedRequest
import play.api.i18n.Messages
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

@Singleton
class CtPartialBuilderImpl @Inject() (appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) extends CtPartialBuilder {

  override def buildReturnsPartial()(implicit request: AuthenticatedRequest[_], messages: Messages): Html =
    views.html.partials.card.returns.potential_returns(appConfig)

  override def buildPaymentsPartial(ctData: Option[CtData])(implicit request: AuthenticatedRequest[_], messages: Messages): Html = {
    ctData match {
      case Some(CtData(accountSummaryData)) => accountSummaryData match {
        case CtAccountSummaryData(Some(CtAccountBalance(Some(amount)))) =>
          if (amount > 0) {
            views.html.partials.card.payments.in_debit(amount.abs, appConfig)
          }
          else if (amount == 0) {
            views.html.partials.card.payments.no_balance(appConfig)
          }
          else {
            views.html.partials.card.payments.in_credit(amount.abs, appConfig)
          }
        case _ => Html("")
      }
      case None => views.html.partials.card.payments.no_data(appConfig)
    }
  }

}


@ImplementedBy(classOf[CtPartialBuilderImpl])
trait CtPartialBuilder {
  def buildReturnsPartial()(implicit request: AuthenticatedRequest[_], messages: Messages): Html
  def buildPaymentsPartial(ctData: Option[CtData])(implicit request: AuthenticatedRequest[_], messages: Messages): Html
}
