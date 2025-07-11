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

package connectors

import _root_.models.UserEnrolments
import config.FrontendAppConfig
import play.api.http.Status
import play.api.libs.json.JsSuccess
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.LoggingUtil

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class EnrolmentStoreConnector @Inject()(val http: HttpClientV2, config: FrontendAppConfig)
                                       (implicit val ec: ExecutionContext) extends LoggingUtil{

  def getEnrolments(credId: String)(implicit headerCarrier: HeaderCarrier, request: Request[_]): Future[Either[String, UserEnrolments]] = {
    infoLog(s"[EnrolmentStoreConnector][getEnrolments] - Attempted to retrieve enrolments")
    http.get(buildURL(credId)).execute[HttpResponse].map { response =>
      response.status match {
        case Status.OK =>
          response.json.validate[UserEnrolments] match {
            case JsSuccess(userEnrolments, _) =>
              Right(userEnrolments)
            case _ =>
              warnLog(s"[EnrolmentStoreConnector][getEnrolments] - Failed with: Unable to parse data from enrolment API")
              Left("Unable to parse data from enrolment API")
          }
        case _ =>
          warnLog(s"[EnrolmentStoreConnector][getEnrolments] - Failed with: ${errorMessage(response)}")
          Left(errorMessage(response))
      }
    }.recover {
      case e: Exception =>
        warnLog(s"[EnrolmentStoreConnector][getEnrolments] - Failed with: ${e.getMessage}")
        Left("Exception thrown from enrolment API")
    }
  }

  def errorMessage(response: HttpResponse): String =
    response.status match {
      case Status.NOT_FOUND => "User not found from enrolment API"
      case Status.BAD_REQUEST => "Bad request to enrolment API"
      case Status.FORBIDDEN => "Forbidden from enrolment API"
      case Status.SERVICE_UNAVAILABLE => "Unexpected error from enrolment API"
      case Status.NO_CONTENT => "No content from enrolment API"
      case _ => "Enrolment API couldn't handle response code"
    }

  private def buildURL(credId: String): URL = url"${config.enrolmentStoreUrl}/enrolment-store/users/$credId/enrolments?service=IR-CT"

}
