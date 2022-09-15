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

package models

import models.payments.PaymentRecord
import play.api.libs.json.{Format, Json, OFormat}

case class Link(href: String,
                id: String,
                title: String,
                dataSso: Option[String] = None,
                external: Boolean = false)

object Link {
  implicit val linkFormat: OFormat[Link] = Json.format[Link]
}

case class Card(title: String,
                description: String,
                referenceNumber: String,
                primaryLink: Option[Link] = None,
                messageReferenceKey: Option[String],
                additionalLinks: Seq[Link] = Nil,
                paymentsPartial: Option[String] = None,
                returnsPartial: Option[String] = None,
                paymentHistory: Either[PaymentRecordFailure.type, List[PaymentRecord]] = Right(Nil),
                paymentSectionAdditionalLinks: Option[List[Link]] = None,
                accountBalance: Option[BigDecimal] = None)

object Card {
  implicit def eitherErrorOrPaymentRecordFormat:  Format[Either[PaymentRecordFailure.type, List[PaymentRecord]]] = PaymentRecord.eitherPaymentHistoryFormatter()

  implicit def paymentRecordsReads()(implicit request: AuthenticatedRequest[_]): Reads[PaymentRecord] = (
    (__ \ "reference").read[String] and
      (__ \ "amountInPence").read[Long] and
      (__ \ "createdOn").read[OffsetDateTime](offsetDateTimeFromLocalDateTimeFormatReads()) and
      (__ \ "taxType").read[String]
    ) (PaymentRecord.apply(_, _, _, _))

  implicit def paymentRecordsWrites: Writes[PaymentRecord] = Writes { paymentRecord =>
    JsObject(Seq(
      "reference" -> JsString(paymentRecord.reference),
      "amountInPence" -> JsNumber(paymentRecord.amountInPence),
      "createdOn" -> Json.toJson(paymentRecord.createdOn)(dateTimeWrites),
      "taxType" -> JsString(paymentRecord.taxType),
    ))
  }

  implicit def cardFormat(implicit request): OFormat[Card] = Json.format[Card]
}

case object PaymentRecordFailure
