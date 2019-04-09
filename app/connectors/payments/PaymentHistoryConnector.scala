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

package connectors.payments

import javax.inject.Singleton

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.payments.{PaymentHistory, PaymentHistoryInterface, PaymentHistoryNotFound}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}



@Singleton
class PaymentHistoryConnector @Inject()(val http: HttpClient, config: FrontendAppConfig) extends PaymentHistoryConnectorInterface{
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val host = config.baseUrl("pay-api")

  val searchScope: String = "BTA"

  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier): Future[Either[String, PaymentHistoryInterface]] = {
    http.GET[HttpResponse](buildUrl(searchTag)).map {
      r => r.status match {
        case 200 => {
          Try(r.json.as[PaymentHistory]) match {
            case Success(data) => Right(data)
            case Failure(_) => Left("unable to parse data from payment api")
          }
        }
        case _ => Left("couldn't handle response from payment api")
      }
    }
      .recover({
        case _ : NotFoundException => Right(PaymentHistoryNotFound)
        case _ : BadRequestException => Left("invalid request sent")
        case _ : Exception => {
          Left("exception thrown from payment api")
        }
      })
  }

  private def buildUrl(searchTag: String, taxType: String = "corporation-tax") = s"$host/pay-api/payment/search/$searchScope/$searchTag?taxType=$taxType"
}

@ImplementedBy(classOf[PaymentHistoryConnector])
trait PaymentHistoryConnectorInterface {
  def get(searchTag: String)(implicit headerCarrier: HeaderCarrier): Future[Either[String, PaymentHistoryInterface]]
}

