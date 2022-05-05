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

package models.payments

import models.PaymentRecordFailure
import models.payments.PaymentRecord.DateFormatting
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json._
import play.twirl.api.HtmlFormat
import utils.{CurrencyFormatter, DateUtil}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, OffsetDateTime}

case class PaymentRecord(reference: String,
                         amountInPence: Long,
                         createdOn: OffsetDateTime,
                         taxType: String) {

  def dateFormatted(implicit messages: Messages): String =
    DateFormatting.formatFull(createdOn.toLocalDate)

  def currencyFormatted: String =
    CurrencyFormatter.formatCurrencyFromPennies(amountInPence)

  def currencyFormattedBold()(implicit messages: Messages): HtmlFormat.Appendable = {
    CurrencyFormatter.formatBoldCurrencyFromPennies(amountInPence)
  }
}

object PaymentRecord extends DateUtil with Logging {

  private[payments] object DateFormatting {
    def formatFull(date: LocalDate)(implicit messages: Messages): String = {
      DateTimeFormatter.ofPattern("d MMMM yyyy").format(date)
    }
  }

  //BTA expects the date time string to be in LocalDateTime format e.g. 2022-04-01T00:00:00.000
  implicit val dateTimeWrites: Writes[OffsetDateTime] = Writes { dateTime =>
    JsString(dateTime.toLocalDateTime.toString)
  }

  implicit lazy val format: OFormat[PaymentRecord] = Json.format[PaymentRecord]

  private def eitherPaymentHistoryReader: Reads[Either[PaymentRecordFailure.type, List[PaymentRecord]]] =
    (json: JsValue) => json.validate[List[PaymentRecord]] match {
      case JsSuccess(validList, jsPath) =>
        logger.debug(s"[PaymentRecord][eitherPaymentHistoryReader] validList: $validList")
        JsSuccess(Right(validList), jsPath)
      case _ =>
        logger.debug(s"[PaymentRecord][eitherPaymentHistoryReader] failed to read payment history: $json")
        JsSuccess(Left(PaymentRecordFailure))
    }

  private[models] val paymentRecordFailureString = "Bad Gateway"

  private def eitherPaymentHistoryWriter: Writes[Either[PaymentRecordFailure.type, List[PaymentRecord]]] = {
    case Right(list) => Json.toJson(list)
    case _ => JsString(paymentRecordFailureString)
  }

  implicit lazy val eitherPaymentHistoryFormatter: Format[Either[PaymentRecordFailure.type, List[PaymentRecord]]] =
    Format(eitherPaymentHistoryReader, eitherPaymentHistoryWriter)

}

case class CtPaymentRecord(reference: String,
                           amountInPence: Long,
                           status: PaymentStatus,
                           createdOn: String, //BTA expects the date time string to be in LocalDateTime format e.g. 2022-04-01T00:00:00.000
                           taxType: String) extends DateUtil with Logging {

  def isValid(createdOn: OffsetDateTime, currentDateTime: OffsetDateTime): Boolean = {
    createdOn.plusDays(7).isAfter(currentDateTime)
  }

  def isSuccessful: Boolean = status == PaymentStatus.Successful

  def validateAndConvertToPaymentRecord(currentDateTime: OffsetDateTime): Option[PaymentRecord] = {
    createdOn.parseOffsetDateTimeFromLocalDateTimeFormat() match {
      case Right(offsetDateTime) if isValid(offsetDateTime, currentDateTime) && isSuccessful =>
        Some(PaymentRecord(
          reference = reference,
          amountInPence = amountInPence,
          createdOn = offsetDateTime,
          taxType = taxType
        ))
      case _ => None
    }

  }
}


object CtPaymentRecord {
  implicit val format: OFormat[CtPaymentRecord] = Json.format[CtPaymentRecord]
}
