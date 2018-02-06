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

import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import controllers.actions._
import config.FrontendAppConfig
import models.{CtEnrolment, CtGenericError, CtNoData}
import models.requests.ServiceInfoRequest
import play.api.mvc.{AnyContent, Request}
import play.twirl.api.{Html, HtmlFormat}
import services.CtService
import uk.gov.hmrc.domain.CtUtr
import views.html.partials.{account_summary, generic_error}
import views.html.subpage

import scala.concurrent.Future

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
    ctService.fetchCtModel(getEnrolment) map {
      case CtNoData => account_summary(Messages("account.summary.nobalance"), appConfig)
      case _ =>  generic_error(appConfig.getPortalUrl("home")(getEnrolment))
    }
  }

  def onPageLoad = (authenticate andThen serviceInfo).async {
    implicit request =>
      getAccountSummaryView.map { accountSummaryView =>
        Ok(subpage(appConfig, getEnrolment(request), accountSummaryView)(request.serviceInfoContent))
      }
  }
}
