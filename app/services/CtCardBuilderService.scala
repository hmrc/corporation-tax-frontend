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
import javax.inject.Inject

import models._
import models.payments.PaymentRecord
import models.requests.AuthenticatedRequest
import org.joda.time.DateTime
import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CtCardBuilderServiceImpl @Inject() (val messagesApi: MessagesApi,
                                          appConfig: FrontendAppConfig,
                                          ctService: CtServiceInterface,
                                          ctPartialBuilder: CtPartialBuilder,
                                          paymentHistoryService: PaymentHistoryServiceInterface
                                         )(implicit ec: ExecutionContext) extends CtCardBuilderService {

  def buildCtCard()(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier, messages: Messages): Future[Card] = {
    val modelHistory = for{
      model <- ctService.fetchCtModel(Some(request.ctEnrolment))
      history <- paymentHistoryService.getPayments(Some(request.ctEnrolment),DateTime.now())
    } yield {
      (model,history)
    }

    modelHistory.map { tuple =>
      val model: CtAccountSummary = tuple._1
      val history: List[PaymentRecord] = tuple._2
      model match {
        case CtNoData => buildCtCardData(
          paymentsContent = Some(ctPartialBuilder.buildPaymentsPartial(None).toString()),
          returnsContent = Some(ctPartialBuilder.buildReturnsPartial().toString()),
          paymentHistory = history
        )
        case data: CtData => buildCtCardData(
          paymentsContent = Some(ctPartialBuilder.buildPaymentsPartial(Some(data)).toString()),
          returnsContent = Some(ctPartialBuilder.buildReturnsPartial().toString()),
          paymentHistory = history
        )
        case _ => throw new Exception
      }
    }
  }

  private def buildCtCardData(paymentsContent: Option[String] = None, returnsContent: Option[String] = None, paymentHistory: List[PaymentRecord])
                           (implicit request: AuthenticatedRequest[_], messages: Messages, hc: HeaderCarrier): Card = {
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
      paymentHistory = paymentHistory
    )
  }
}

@ImplementedBy(classOf[CtCardBuilderServiceImpl])
trait CtCardBuilderService {
  def buildCtCard()(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier, messages: Messages): Future[Card]
}
