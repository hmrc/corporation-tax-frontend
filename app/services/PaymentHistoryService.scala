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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import connectors.payments.PaymentHistoryConnectorInterface
import models.payments.{CtPaymentRecord, PaymentRecord}
import models.{CtEnrolment, PaymentRecordFailure}
import org.joda.time.DateTime
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryService @Inject()(connector: PaymentHistoryConnectorInterface, config: FrontendAppConfig)
                                     (implicit ec: ExecutionContext) extends PaymentHistoryServiceInterface {

  def getPayments(ctEnrolment: CtEnrolment,
                  currentDate: DateTime)(implicit hc: HeaderCarrier): Future[Either[PaymentRecordFailure.type, List[PaymentRecord]]] =
    if (config.getCTPaymentHistoryToggle) {
      connector.get(ctEnrolment.ctUtr.utr).map {
        case Right(payments) => Right(filterPaymentHistory(payments, currentDate))
        case Left(message) => log(message)
      }
    } else {
      Future.successful(Right(Nil))
    }

  private def log(errorMessage: String): Either[PaymentRecordFailure.type, List[PaymentRecord]] = {
    Logger.warn(s"[PaymentHistoryService][getPayments] $errorMessage")
    Left(PaymentRecordFailure)
  }

  private def filterPaymentHistory(payments: List[CtPaymentRecord], currentDate: DateTime): List[PaymentRecord] =
    payments.flatMap(PaymentRecord.from(_, currentDate))

}

@ImplementedBy(classOf[PaymentHistoryService])
trait PaymentHistoryServiceInterface {
  def getPayments(enrolment: CtEnrolment,
                  currentDate: DateTime)(implicit hc: HeaderCarrier): Future[Either[PaymentRecordFailure.type, List[PaymentRecord]]]
}
