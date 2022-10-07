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

import connectors.CtConnector

import javax.inject.{Inject, Singleton}
import models.{CtAccountSummaryData, _}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CtService @Inject()(ctConnector: CtConnector) {

  def fetchCtModel(ctEnrolment: CtEnrolment)(implicit headerCarrier: HeaderCarrier,
                                             ec: ExecutionContext, request: Request[_]): Future[Either[CtAccountFailure, Option[CtData]]] =
    ctEnrolment match {
      case CtEnrolment(utr, true) =>
        ctConnector.accountSummary(utr).map {
          case Some(accountSummary@CtAccountSummaryData(Some(_))) => Right(Some(CtData(accountSummary)))
          case _ => Right(None)
        }.recover {
          case _ => Left(CtGenericError)
        }
      case CtEnrolment(_, false) => Future.successful(Left(CtUnactivated))
      case _ => Future.successful(Left(CtEmpty))
    }

}

