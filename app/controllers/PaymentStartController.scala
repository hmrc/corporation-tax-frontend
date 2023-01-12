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

import java.time.LocalDate
import config.FrontendAppConfig
import connectors.payments.{PaymentConnector, SpjRequestBtaCt}
import controllers.PaymentStartController.toAmountInPence
import controllers.actions._

import javax.inject.Inject
import models.{CtAccountBalance, CtAccountSummaryData, CtData}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CtService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggingUtil
import views.html.partials.generic_error

import scala.concurrent.{ExecutionContext, Future}

object PaymentStartController {
  def toAmountInPence(amountInPounds: BigDecimal): Long = (amountInPounds * 100).toLong

  implicit val localDateOrdering: Ordering[LocalDate] = new Ordering[LocalDate] {
    def compare(x: LocalDate, y: LocalDate): Int = x compareTo y
  }
}

class PaymentStartController @Inject()(appConfig: FrontendAppConfig,
                                       payConnector: PaymentConnector,
                                       authenticate: AuthAction,
                                       ctService: CtService,
                                       mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with LoggingUtil {

  def makeAPayment: Action[AnyContent] = authenticate.async { implicit request =>
    infoLog(s"[PaymentStartController][makeAPayment] - make a payment attempted")
    ctService.fetchCtModel(request.ctEnrolment).flatMap {
      case Right(Some(CtData(CtAccountSummaryData(Some(CtAccountBalance(Some(amount))))))) =>
        val spjRequestBtaVat = SpjRequestBtaCt(
          toAmountInPence(amount),
          appConfig.businessAccountHomeAbsoluteUrl,
          appConfig.businessAccountHomeAbsoluteUrl,
          request.ctEnrolment.ctUtr.utr)
        payConnector.ctPayLink(spjRequestBtaVat).map(response => Redirect(response.nextUrl))
      case _ =>
        errorLog(s"[PaymentStartController][makeAPayment] - Failed to fetch CtModel")
        Future.successful(BadRequest(generic_error(appConfig.getPortalUrl("home")(request.ctEnrolment))))
    }
  }

}
