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

import javax.inject.{Inject, Singleton}
import connectors.EnrolmentStoreConnector
import models._
import models.CtUtr
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreService @Inject()(connector: EnrolmentStoreConnector)(implicit val ec:ExecutionContext) {

  final val daysBetweenExpectedArrivalAndExpiry = 23

  def showNewPinLink(enrolment: CtEnrolment, currentDate: OffsetDateTime)(implicit hc: HeaderCarrier, request: Request[_]): Future[Boolean] = enrolment match {
    case CtEnrolment(CtUtr(utr), false) =>
      val enrolmentDetailsList: Future[Either[String, UserEnrolments]] = connector.getEnrolments(utr)
      enrolmentDetailsList.map{
        case Right(UserEnrolments(enrolmentDataList)) if enrolmentDataList.nonEmpty =>

          val enrolmentStatus: Seq[UserEnrolmentStatus] = enrolmentDataList.filter(_.enrolmentTokenExpiryDate.isDefined).sortWith { (left, right) =>
            left.enrolmentTokenExpiryDate.get.isAfter(right.enrolmentTokenExpiryDate.get)
          }

          enrolmentStatus match {
            case Nil => true
            case _ =>
              val expectedArrivalDate: OffsetDateTime =
                enrolmentStatus.head.enrolmentTokenExpiryDate.get.minusDays(daysBetweenExpectedArrivalAndExpiry).atOffset(ZoneOffset.UTC)
              currentDate.isAfter(expectedArrivalDate)
          }
        case _ => true
      }
    case CtEnrolment(_,true) => Future.successful(false)
    case _ => Future.successful(true)
  }
}

