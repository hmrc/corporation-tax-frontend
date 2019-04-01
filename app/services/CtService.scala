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
import connectors.CtConnector
import connectors.models.{CtAccountSummaryData, CtDesignatoryDetailsCollection}
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CtService @Inject()(ctConnector: CtConnector) extends CtServiceInterface {

  def fetchCtModel(ctEnrolmentOpt: Option[CtEnrolment])(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[CtAccountSummary] = {
    ctEnrolmentOpt match {
      case Some(enrolment@CtEnrolment(utr, true)) =>
        ctConnector.accountSummary(utr).map {
          case Some(accountSummary@CtAccountSummaryData(Some(_))) => CtData(accountSummary)
          case _ => CtNoData
        }.recover {
          case _ => CtGenericError
        }
      case Some(enrolment@CtEnrolment(utr, false)) => Future(CtUnactivated)
      case _ => Future(CtEmpty)
    }
  }

  def designatoryDetails(ctEnrolment: CtEnrolment)
                        (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[CtDesignatoryDetailsCollection]] = {
    ctConnector.designatoryDetails(ctEnrolment.ctUtr).recover {
      case e  =>
        Logger.warn(s"Failed to fetch ct designatory details with message - ${e.getMessage}")
        None
    }
  }

}

@ImplementedBy(classOf[CtService])
trait CtServiceInterface {
  def fetchCtModel(ctEnrolmentOpt: Option[CtEnrolment])(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[CtAccountSummary]
}
