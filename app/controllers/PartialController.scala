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
import controllers.actions._
import models.Card
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.partial
import play.api.libs.json.Json.toJson

class PartialController @Inject()(override val messagesApi: MessagesApi,
                                  authenticate: AuthAction,
                                  serviceInfo: ServiceInfoAction,
                                  accountSummaryHelper: AccountSummaryHelper,
                                 appConfig: FrontendAppConfig
                                 ) extends FrontendController with I18nSupport {


  def onPageLoad = authenticate.async  {
    implicit request =>
      accountSummaryHelper.getAccountSummaryView(showCreditCardMessage = false).map { accountSummaryView =>
        Ok(partial(request.ctEnrolment.ctUtr, accountSummaryView, appConfig))
      }
  }

  def getCard = authenticate.async {
    implicit request =>
      accountSummaryHelper.getAccountSummaryView(true).map { _ =>
        Ok(toJson(Card(title = messagesApi.preferred(request)("partial.heading"),
          body = messagesApi.preferred(request)("partial.more_details"))))
      } recover{
        case _ => InternalServerError("Failed to get data from backend")
      }
  }
}