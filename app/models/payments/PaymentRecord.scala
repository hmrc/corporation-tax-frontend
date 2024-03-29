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

import models.PaymentRecordFailure
import models.payments.PaymentRecord.DateFormatting
import models.requests.AuthenticatedRequest
import play.api.i18n.Messages
import play.api.libs.json._
import play.twirl.api.HtmlFormat
import utils.{CurrencyFormatter, DateUtil, LoggingUtil}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, OffsetDateTime}

case class PaymentRecord(reference: String,
                         amountInPence: Long,
                         createdOn: OffsetDateTime,
                         taxType: String) {

  def dateFormatted: String =
    DateFormatting.formatFull(createdOn.toLocalDate)

  def currencyFormatted: String =
    CurrencyFormatter.formatCurrencyFromPennies(amountInPence)

  def currencyFormattedBold()(implicit messages: Messages): HtmlFormat.Appendable = {
    CurrencyFormatter.formatBoldCurrencyFromPennies(amountInPence)
  }
}

object PaymentRecord extends DateUtil with LoggingUtil {

  private[payments] object DateFormatting {
    def formatFull(date: LocalDate): String = {
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
    case Right(list) =>
      logger.debug(s"[PaymentRecord][eitherPaymentHistoryWriter] validList: $list")
      Json.toJson(list)
    case Left(error) =>
      logger.debug(s"[PaymentRecord][eitherPaymentHistoryWriter] failed to write payment history: $error")
      JsString(paymentRecordFailureString)
  }

  implicit lazy val eitherPaymentHistoryFormatter: Format[Either[PaymentRecordFailure.type, List[PaymentRecord]]] =
    Format(eitherPaymentHistoryReader, eitherPaymentHistoryWriter)

}

case class CtPaymentRecord(reference: String,
                           amountInPence: Long,
                           status: PaymentStatus,
                           createdOn: String, //BTA expects the date time string to be in LocalDateTime format e.g. 2022-04-01T00:00:00.000
                           taxType: String) extends DateUtil with LoggingUtil {

  def isValid(createdOn: OffsetDateTime, currentDateTime: OffsetDateTime): Boolean = {
    createdOn.plusDays(7).isAfter(currentDateTime)
  }

  def isSuccessful: Boolean = status == PaymentStatus.Successful

  def validateAndConvertToPaymentRecord(currentDateTime: OffsetDateTime)(implicit request: AuthenticatedRequest[_]): Option[PaymentRecord] = {
    createdOn.parseOffsetDateTimeFromLocalDateTimeFormat() match {
      case Right(offsetDateTime) if isValid(offsetDateTime, currentDateTime) && isSuccessful =>
        infoLog(s"[CtPaymentRecord][validateAndConvertToPaymentRecord] - validated and converted to PaymentRecord")
        Some(PaymentRecord(
          reference = reference,
          amountInPence = amountInPence,
          createdOn = offsetDateTime,
          taxType = taxType
        ))
      case Left(error) =>
        warnLog(s"[DateUtil][parseOffsetDateTimeFromLocalDateTimeFormat] could not parse createdOn dateTime" +
          s"\n createdOn: ${error.dateFailedToParse}" +
          s"\n exception: ${error.errorMessage}"
        )
        None
      case _ => None
    }

  }
}

  object CtPaymentRecord {
    implicit val format: OFormat[CtPaymentRecord] = Json.format[CtPaymentRecord]
  }
