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

package services

import config.FrontendAppConfig
import connectors.payments.PaymentHistoryConnector
import models.payments.{CtPaymentRecord, PaymentRecord}
import models.requests.{AuthenticatedRequest, ServiceInfoRequest}
import models.{CtEnrolment, PaymentRecordFailure}
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingUtil

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryService @Inject()(connector: PaymentHistoryConnector, config: FrontendAppConfig)
                                     (implicit ec: ExecutionContext) extends LoggingUtil {

  def getPayments(ctEnrolment: CtEnrolment, currentDate: OffsetDateTime)
                 (implicit hc: HeaderCarrier,request: AuthenticatedRequest[_]): Future[Either[PaymentRecordFailure.type, List[PaymentRecord]]] =
    connector.get(ctEnrolment.ctUtr.utr).map {
      case Right(payments) =>
        Right(filterPaymentHistory(payments, currentDate))
      case Left(message) => log(message)
    }

  private def log[A](errorMessage: String)(implicit request: AuthenticatedRequest[_]): Either[PaymentRecordFailure.type, List[PaymentRecord]] = {
    warnLog(s"[PaymentHistoryService][getPayments] $errorMessage")
    Left(PaymentRecordFailure)
  }

  private def filterPaymentHistory(payments: List[CtPaymentRecord], currentDate: OffsetDateTime)(implicit request: AuthenticatedRequest[_]): List[PaymentRecord] =
    payments.flatMap(_.validateAndConvertToPaymentRecord(currentDate))

}
