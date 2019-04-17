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
import controllers.actions._
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent}
import play.api.mvc.AnyContent
import services.CtCardBuilderService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class PartialController @Inject()(override val messagesApi: MessagesApi,
                                  authenticate: AuthAction,
                                  serviceInfo: ServiceInfoAction,
                                  accountSummaryHelper: AccountSummaryHelper,
                                  appConfig: FrontendAppConfig,
                                  ctCardBuilderService: CtCardBuilderService
                                 )(implicit ec: ExecutionContext) extends FrontendController with I18nSupport {

  def onPageLoad: Action[AnyContent] = authenticate.async { implicit request =>
    accountSummaryHelper.getAccountSummaryView(showCreditCardMessage = false).map { accountSummaryView =>
      Ok(views.html.partial(request.ctEnrolment.ctUtr, accountSummaryView, appConfig,request.ctEnrolment.isActivated ))
    }
  }

  def getCard: Action[AnyContent] = authenticate.async { implicit request =>
    ctCardBuilderService.buildCtCard().map( card => {
      Ok(toJson(card))
    }).recover {
      case _: Exception => InternalServerError("Failed to get data from backend")
    }
  }

}
