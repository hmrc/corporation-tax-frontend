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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import connectors.payments.PaymentHistoryConnectorInterface
import models.CtEnrolment
import models.payments.{PaymentHistory, PaymentHistoryNotFound, PaymentRecord}
import org.joda.time.DateTime
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryService @Inject()(connector: PaymentHistoryConnectorInterface, config: FrontendAppConfig)
                                     (implicit ec: ExecutionContext) extends PaymentHistoryServiceInterface {
  def getPayments(enrolment: Option[CtEnrolment], currentDate: DateTime)(implicit hc: HeaderCarrier): Future[List[PaymentRecord]] = {
    if(config.getCTPaymentHistoryToggle) {
      enrolment match {

        case Some(ctEnrolment) => {
          connector.get(ctEnrolment.ctUtr.utr).map {
            case Right(PaymentHistoryNotFound) => Nil
            case Right(PaymentHistory(_, _, payments)) => filterPaymentHistory(payments, currentDate)
            case Right(_) => Nil
            case Left(message) => log(message)
          }.recover {
            case _ => Nil
          }
        }
        case None => Future.successful(Nil)
      }
    } else {
      Future.successful(Nil)
    }
  }

  private def log(x: String): List[PaymentRecord] = {
    Logger.warn(s"[PaymentHistoryService][getPayments] $x")
    Nil
  }

  private def filterPaymentHistory(payments: List[PaymentRecord], currentDate: DateTime): List[PaymentRecord] = {
    payments.filter(_.isValid(currentDate)).filter(_.isSuccessful)
  }
}

@ImplementedBy(classOf[PaymentHistoryService])
trait PaymentHistoryServiceInterface {
  def getPayments(enrolment: Option[CtEnrolment], currentDate: DateTime)(implicit hc: HeaderCarrier): Future[List[PaymentRecord]]
}
