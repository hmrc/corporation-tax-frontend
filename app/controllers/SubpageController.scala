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
import controllers.actions._
import javax.inject.Inject
import models.requests.{AuthenticatedRequest, ServiceInfoRequest}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.subpage

import scala.concurrent.{ExecutionContext, Future}

class SubpageController @Inject()(appConfig: FrontendAppConfig,
                                  mcc: MessagesControllerComponents,
                                  authenticate: AuthAction,
                                  serviceInfo: ServiceInfoAction,
                                  subpage: subpage,
                                  accountSummaryHelper: AccountSummaryHelper)
                                 (implicit ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {


  def onPageLoad: Action[AnyContent] = (authenticate andThen serviceInfo).async { implicit request =>
    getAccountSummaryView(request)
  }

  private def getAccountSummaryView(request: ServiceInfoRequest[_]): Future[Result] = {
    implicit val requestToUse: AuthenticatedRequest[_] = request.request

    accountSummaryHelper.getAccountSummaryView().map { accountSummaryView =>
      Ok(subpage(appConfig, request.request.ctEnrolment, accountSummaryView)(request.serviceInfoContent))
    }
  }
}
