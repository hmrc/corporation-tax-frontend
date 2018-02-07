/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.actions._
import models.requests.ServiceInfoRequest
import models.{CtData, CtEnrolment, CtNoData}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import services.CtService
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.views.formatting.Money.pounds
import views.html.partials.{account_summary, generic_error}
import views.html.subpage

class SubpageController @Inject()(appConfig: FrontendAppConfig,
                                  ctService: CtService,
                                  override val messagesApi: MessagesApi,
                                  authenticate: AuthAction,
                                  serviceInfo: ServiceInfoAction) extends FrontendController with I18nSupport {

  private[controllers] def getEnrolment(implicit r: ServiceInfoRequest[_]): Option[CtEnrolment] = {
    r.request.enrolments.getEnrolment("IR-CT")
      .map(e => CtEnrolment(CtUtr(e.getIdentifier("UTR").map(_.value).getOrElse("")), e.isActivated))
  }

  private[controllers] def getAccountSummaryView(implicit r: ServiceInfoRequest[_]) = {

    val breakdownLink = Some(appConfig.getPortalUrl("balance")(getEnrolment))

    ctService.fetchCtModel(getEnrolment) map {
      case CtData(accountSummaryData) => accountSummaryData match {
        case CtAccountSummaryData(Some(CtAccountBalance(Some(amount)))) =>
          if (amount < 0) {
            account_summary(
              Messages("account.summary.incredit", pounds(amount.abs, 2)),
              appConfig,
              breakdownLink, Messages("account.summary.seebreakdown")
            )
          } else if (amount == 0) {
            account_summary(
              Messages("account.summary.nothingtopay"),
              appConfig,
              breakdownLink, Messages("account.summary.viewstatement")
            )
          } else {
            account_summary(
              Messages("account.summary.indebit", pounds(amount.abs, 2)),
              appConfig,
              breakdownLink, Messages("account.summary.seebreakdown"),
              panelIndent = true
            )
          }
        case _ => account_summary(
          Messages("account.summary.nothingtopay"),
          appConfig,
          breakdownLink, Messages("account.summary.viewstatement")
        )
      }
      case CtNoData => account_summary(Messages("account.summary.nobalance"), appConfig)
      case _ => generic_error(appConfig.getPortalUrl("home")(getEnrolment))
    }
  }

  def onPageLoad = (authenticate andThen serviceInfo).async {
    implicit request =>
      getAccountSummaryView.map { accountSummaryView =>
        Ok(subpage(appConfig, getEnrolment(request), accountSummaryView)(request.serviceInfoContent))
      }
  }
}
