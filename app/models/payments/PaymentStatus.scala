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

package models.payments

import play.api.libs.json._

sealed trait PaymentStatus

object PaymentStatus {

  case object Successful extends PaymentStatus

  case object Invalid extends PaymentStatus

  private[payments] object ApiStatusCode {
    val Created = "created"
    val Successful = "successful"
    val Sent = "sent"
    val Failed = "failed"
    val Cancelled = "cancelled"
    val Invalid = "invalid"
  }

  implicit def paymentStatusReads: Reads[PaymentStatus] = new Reads[PaymentStatus] {
    override def reads(json: JsValue): JsResult[PaymentStatus] =
      json match {
        case JsString(string) =>
          string.toLowerCase match {
            case ApiStatusCode.Successful => JsSuccess(Successful)
            case _ => JsSuccess(Invalid)
          }
        case _ => JsError()
      }
  }

  implicit def paymentStatusWrites: Writes[PaymentStatus] = new Writes[PaymentStatus] {def writes(paymentStatus: PaymentStatus): JsString = paymentStatus match {
      case Successful => JsString(ApiStatusCode.Successful)
      case Invalid => JsString(ApiStatusCode.Invalid)
    }
  }

}
