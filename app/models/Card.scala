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

package models

import models.payments.PaymentRecord
import play.api.libs.json.{Json, OFormat}

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
  implicit val cardFormat: OFormat[Card] = Json.format[Card]
}

case object PaymentRecordFailure
