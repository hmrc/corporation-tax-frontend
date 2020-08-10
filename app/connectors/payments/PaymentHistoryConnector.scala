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

package connectors.payments

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import models.payments.CtPaymentRecord
import play.api.http.Status._
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpResponse, NotFoundException}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.Future

@Singleton
class PaymentHistoryConnector @Inject()(val http: HttpClient, config: FrontendAppConfig) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier): Future[Either[String, List[CtPaymentRecord]]] =
    http.GET[HttpResponse](buildUrl(searchTag)).map { response =>
      response.status match {
        case OK =>
          (response.json \ "payments").validate[List[CtPaymentRecord]] match {
            case JsSuccess(paymentHistory, _) => Right(paymentHistory)
            case _ => Left("unable to parse data from payment api")
          }
        case _ => Left("couldn't handle response from payment api")
      }
    }.recover {
      case _: NotFoundException => Right(Nil)
      case _: BadRequestException => Left("invalid request sent")
      case _: Exception =>
        Left("exception thrown from payment api")
    }

  private def buildUrl(searchTag: String) = s"${config.payApiUrl}/pay-api/payment/search/BTA/$searchTag?taxType=corporation-tax"

}
