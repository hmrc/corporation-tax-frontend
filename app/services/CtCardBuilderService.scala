/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.Inject
import models._
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import org.joda.time.DateTime
import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CtCardBuilderService @Inject()(val messagesApi: MessagesApi,
                                     appConfig: FrontendAppConfig,
                                     ctService: CtService,
                                     ctPartialBuilder: CtPartialBuilder,
                                     paymentHistoryService: PaymentHistoryService
                                    )(implicit ec: ExecutionContext) {

  def buildCtCard()(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier, messages: Messages): Future[Card] =
    for {
      model <- ctService.fetchCtModel(request.ctEnrolment)
      history <- paymentHistoryService.getPayments(request.ctEnrolment, DateTime.now())
    } yield {
      model match {
        case Right(None) => buildCtCardData(
          paymentsContent = Some(ctPartialBuilder.buildPaymentsPartial(None).toString()),
          returnsContent = Some(ctPartialBuilder.buildReturnsPartial().toString()),
          paymentHistory = history
        )
        case Right(Some(data)) => buildCtCardData(
          paymentsContent = Some(ctPartialBuilder.buildPaymentsPartial(Some(data)).toString()),
          returnsContent = Some(ctPartialBuilder.buildReturnsPartial().toString()),
          paymentHistory = history
        )
        case _ => throw new Exception
      }
    }

  private def buildCtCardData(
                               paymentsContent: Option[String] = None,
                               returnsContent: Option[String] = None,
                               paymentHistory: Either[PaymentRecordFailure.type, List[PaymentRecord]]
                             )(
                               implicit request: AuthenticatedRequest[_],
                               messages: Messages,
                               hc: HeaderCarrier
                             ): Card = {
    Card(
      title = messagesApi.preferred(request)("partial.heading"),
      description = "",
      referenceNumber = request.ctEnrolment.ctUtr.utr,
      primaryLink = Some(
        Link(
          href = appConfig.getUrl("mainPage"),
          ga = "link - click:CT cards:More CT details",
          id = "ct-account-details-card-link",
          title = messagesApi.preferred(request)("partial.heading")
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
            ga = "link - click:CT cards:View your CT statement",
            id = "view-ct-statement",
            title = messagesApi.preferred(request)("card.view_your_corporation_tax_statement")
          ),
          Link(
            href = s"${appConfig.getUrl("mainPage")}/make-a-payment",
            ga = "link - click:CT cards:Make a CT payment",
            id = "make-ct-payment",
            title = messagesApi.preferred(request)("card.make_a_corporation_tax_payment")
          )
        )
      )
    )
  }
}
