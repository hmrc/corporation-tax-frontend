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
import models.payments.CtPaymentRecord
import play.api.http.Status._
import play.api.libs.json.JsSuccess
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException, StringContextOps}
import utils.LoggingUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryConnector @Inject()(val http: HttpClientV2, config: FrontendAppConfig) extends LoggingUtil{


  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier,  ec: ExecutionContext, request: Request[_]): Future[Either[String, List[CtPaymentRecord]]] =
    http.get(buildUrl(searchTag)).execute[HttpResponse].map { response =>
      infoLog(s"[PaymentHistoryConnector][get] - Attempted to retrieve payment history")
      response.status match {
        case OK =>
          (response.json \ "payments").validate[List[CtPaymentRecord]] match {
            case JsSuccess(paymentHistory: Seq[CtPaymentRecord], _) =>
              infoLog(s"[PaymentHistoryConnector][get] -retrieve payment history successful")
              Right(paymentHistory)
            case _ =>
              warnLog(s"[PaymentHistoryConnector][get] - Failed with: unable to parse data from payment api")
              Left("unable to parse data from payment api")
          }
        case _ =>
          warnLog(s"[PaymentHistoryConnector][get] - Failed with: couldn't handle response from payment api")
          Left("couldn't handle response from payment api")
      }
    }.recover {
      case _: NotFoundException => Right(Nil)
      case e: BadRequestException =>
        errorLog(s"[PaymentHistoryConnector][get] - Failed with: ${e.getMessage}")
        Left("invalid request sent")
      case e: Exception =>
        errorLog(s"[PaymentHistoryConnector][get] - Failed with: ${e.getMessage}")
        Left("exception thrown from payment api")
    }

  private def buildUrl(searchTag: String) = url"${config.payApiUrl}/pay-api/v2/payment/search/$searchTag?taxType=corporation-tax&searchScope=BTA"

}
