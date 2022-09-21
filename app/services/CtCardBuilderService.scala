/*
 * Copyright 2022 HM Revenue & Customs
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

import config.FrontendAppConfig
import models._
import models.payments.PaymentRecord
import models.requests.{AuthenticatedRequest, ServiceInfoRequest}
import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingUtil

import java.time.OffsetDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CtCardBuilderService @Inject()(val messagesApi: MessagesApi,
                                     appConfig: FrontendAppConfig,
                                     ctService: CtService,
                                     ctPartialBuilder: CtPartialBuilder,
                                     paymentHistoryService: PaymentHistoryService)(implicit ec: ExecutionContext) extends LoggingUtil{

  def buildCtCard()(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier, messages: Messages): Future[Card] = {
    for {
      model <- ctService.fetchCtModel(request.ctEnrolment)
      history <- paymentHistoryService.getPayments(request.ctEnrolment, OffsetDateTime.now())
    } yield {
      logger.debug(s"[CtCardBuilderService][buildCtCard]" +
        s"\n model: $model," +
        s"\n history: $history"
      )
      model match {
        case Right(None) => buildCtCardData(
          paymentsContent = Some(ctPartialBuilder.buildPaymentsPartial(None).toString()),
          returnsContent = Some(ctPartialBuilder.buildReturnsPartial().toString()),
          paymentHistory = history,
          ctAccountBalance = None
        )
        case Right(Some(data)) => buildCtCardData(
          paymentsContent = Some(ctPartialBuilder.buildPaymentsPartial(Some(data)).toString()),
          returnsContent = Some(ctPartialBuilder.buildReturnsPartial().toString()),
          paymentHistory = history,
          ctAccountBalance = data.accountSummary.accountBalance.flatMap(_.amount)
        )
        case _ =>
          warnLog(s"[CtCardBuilderService][buildCtCard] failed to build card")
          throw new Exception
      }
    }
  }

  private def buildCtCardData(paymentsContent: Option[String],
                              returnsContent: Option[String],
                              paymentHistory: Either[PaymentRecordFailure.type, List[PaymentRecord]],
                              ctAccountBalance: Option[BigDecimal])
                             (implicit request: AuthenticatedRequest[_], messages: Messages): Card = {
    Card(
      title = messages("partial.heading"),
      description = "",
      referenceNumber = request.ctEnrolment.ctUtr.utr,
      primaryLink = Some(
        Link(
          href = appConfig.getUrl("mainPage"),
          id = "ct-account-details-card-link",
          title = messages("partial.heading")
        )
      ),
      messageReferenceKey = Some("card.ct.utr"),
      paymentsPartial = paymentsContent,
      returnsPartial = returnsContent,
      paymentHistory = paymentHistory,
      paymentSectionAdditionalLinks = Some(
        List(
          Link(
            href = appConfig.getPortalUrl("balance")(request.ctEnrolment),
            id = "view-ct-statement",
            title = messages("card.view_your_corporation_tax_statement")
          ),
          Link(
            href = s"${appConfig.getUrl("mainPage")}/make-a-payment",
            id = "make-ct-payment",
            title = messages("card.make_a_corporation_tax_payment")
          )
        )
      ),
      accountBalance = ctAccountBalance
    )
  }
}
