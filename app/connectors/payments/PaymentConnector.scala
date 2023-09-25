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

package connectors.payments

import config.FrontendAppConfig
import models.SpjRequestBtaCt
import play.api.libs.json.{Format, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}
import utils.LoggingUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentConnector @Inject()(http: HttpClient, config: FrontendAppConfig) extends LoggingUtil {
  private def payApiBaseUrl: String = config.payApiUrl

  private def paymentsFrontendBaseUrl: String = config.getUrl("paymentsFrontendBase")

  def ctPayLink(spjRequest: SpjRequestBtaCt)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[NextUrl] = {
    infoLog(s"[PaymentConnector][ctPayLink] - ctPayLink attempted. Amount and dueDate provided")

    http.POST[SpjRequestBtaCt, NextUrl](s"$payApiBaseUrl/pay-api/bta/ct/journey/start", spjRequest)
      .recover {
        case e: Exception =>
          errorLog(s"[PaymentConnector][ctPayLink] - Error: ${e.getMessage}")
          NextUrl(s"$paymentsFrontendBaseUrl/service-unavailable")
      }
  }
}

final case class NextUrl(nextUrl: String)

object NextUrl {
  implicit val nextUrlFormat: Format[NextUrl] = Json.format[NextUrl]
}
