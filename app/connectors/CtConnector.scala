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

import config.FrontendAppConfig

import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import models._
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.LoggingUtil


@Singleton
class CtConnector @Inject()(val http: HttpClientV2,
                            val config: FrontendAppConfig) extends LoggingUtil {

  lazy val ctUrl: String = config.ctUrl

  private def handleResponse[A](uri: String)(implicit rds: HttpReads[A], request: Request[_]): HttpReads[Option[A]] = new HttpReads[Option[A]] {
    infoLog(s"[CtConnector][handleResponse] - Attempted with uri: $uri")

    override def read(method: String, url: String, response: HttpResponse): Option[A] = response.status match {
      case OK =>
        Some(rds.read(method, url, response))
      case NO_CONTENT | NOT_FOUND =>
        warnLog(s"[CtConnector][handleResponse] - Failed with: ${NO_CONTENT | NOT_FOUND}")
        None
      case _ =>
        errorLog(s"[CtConnector][handleResponse] - Unexpected response status: ${response.status} (possible further details: ${response.body}) for call to $uri")
        throw MicroServiceException(
          s"Unexpected response status: ${response.status} (possible further details: ${response.body}) for call to $uri",
          response
        )
    }
  }

  def accountSummary(ctUtr: CtUtr)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[Option[CtAccountSummaryData]] = {
    val uri = s"$ctUrl/ct/$ctUtr/account-summary"
    infoLog(s"[CtConnector][accountSummary] - Attempted to retrieve account summary")
    http.get(url"$uri").execute(handleResponse[CtAccountSummaryData](uri), ec)
  }

  def designatoryDetails(ctUtr: CtUtr)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[Option[CtDesignatoryDetailsCollection]] = {
    val uri = ctUrl + s"/ct/$ctUtr/designatory-details"
    infoLog(s"[CtConnector][designatoryDetails] - Attempted to retrieve designatory details")
    http.get(url"$uri").execute(handleResponse[CtDesignatoryDetailsCollection](uri), ec)
  }

}