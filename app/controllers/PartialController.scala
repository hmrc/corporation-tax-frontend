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
import play.api.i18n.I18nSupport
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CtCardBuilderService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggingUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PartialController @Inject()(mcc: MessagesControllerComponents,
                                  authenticate: AuthAction,
                                  serviceInfo: ServiceInfoAction,
                                  accountSummaryHelper: AccountSummaryHelper,
                                  appConfig: FrontendAppConfig,
                                  ctCardBuilderService: CtCardBuilderService)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with LoggingUtil {

  def getCard: Action[AnyContent] = authenticate.async { implicit request =>
    ctCardBuilderService.buildCtCard().map(card => {
      logger.debug(s"[PartialController][getCard] $card")
      Ok(toJson(card))
    }).recover {
      case e: Exception =>
        errorLog(s"[PartialController][getCard] - Failed with: ${e.getMessage}")
        InternalServerError("Failed to get data from backend")
    }
  }

}
