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
import models.payments.PaymentRecord.{DateFormatting, paymentRecordsWrites}
import models.requests.AuthenticatedRequest
import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json._
import play.twirl.api.HtmlFormat
import utils.{CurrencyFormatter, DateUtil, LoggingUtil}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, OffsetDateTime, ZoneId}
import scala.util.Try

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

object PaymentRecord extends DateUtil with LoggingUtil {

  private[payments] object DateFormatting {
    def formatFull(date: LocalDate)(implicit messages: Messages): String = {
      DateTimeFormatter.ofPattern("d MMMM yyyy").format(date)
    }
  }


  //bta expects the date time string to be in localdatetime format e.g. 2021-04-01t00:00:00.000
  implicit val dateTimeWrites: Writes[OffsetDateTime] = Writes { dateTime =>
    JsString(dateTime.toLocalDateTime.toString)
  }
//  implicit val offsetDateTimeFromLocalDateTimeFormatReads: Reads[OffsetDateTime] = offsetDateTimeFromLocalDateTimeFormatReads()
  //  (implicit request: AuthenticatedRequest[_] )
  implicit def paymentRecordsReads()(implicit request: AuthenticatedRequest[_]) : Reads[PaymentRecord] = (
    (__ \ "reference").read[String] and
      (__ \ "amountInPence").read[Long] and
      (__ \ "createdOn").read[OffsetDateTime](offsetDateTimeFromLocalDateTimeFormatReads()) and
      (__ \ "taxType").read[String]
    )(PaymentRecord.apply(_, _, _, _))
 implicit def paymentRecordsWrites : Writes[PaymentRecord] = Writes{ paymentRecord =>
     JsObject(Seq(
       "reference" -> JsString(paymentRecord.reference),
       "amountInPence" -> JsNumber(paymentRecord.amountInPence),
       "createdOn" -> Json.toJson(paymentRecord.createdOn)(dateTimeWrites),
       "taxType" -> JsString(paymentRecord.taxType),
     ))
 }
  implicit def paymentRecordformat()(implicit request: AuthenticatedRequest[_]): Format[PaymentRecord] =
    Format[PaymentRecord](paymentRecordsReads()(request), paymentRecordsWrites)

  private def eitherPaymentHistoryReader()(implicit request: AuthenticatedRequest[_]): Reads[Either[PaymentRecordFailure.type, List[PaymentRecord]]] = {
    implicit val format: Format[PaymentRecord] = paymentRecordformat()(request)
    (json: JsValue) => json.validate[List[PaymentRecord]] match {
      case JsSuccess(validList, jsPath) =>
        logger.debug(s"[PaymentRecord][eitherPaymentHistoryReader] validList: $validList")
        JsSuccess(Right(validList), jsPath)
      case _ =>
        logger.debug(s"[PaymentRecord][eitherPaymentHistoryReader] failed to read payment history: $json")
        JsSuccess(Left(PaymentRecordFailure))
    }
  }

  private[models] val paymentRecordFailureString = "Bad Gateway"

  private def eitherPaymentHistoryWriter: Writes[Either[PaymentRecordFailure.type, List[PaymentRecord]]] = {
    case Right(list) => Json.toJson(list)
    case _ => JsString(paymentRecordFailureString)
  }

  implicit def eitherPaymentHistoryFormatter()(implicit request: AuthenticatedRequest[_]): Format[Either[PaymentRecordFailure.type, List[PaymentRecord]]] = {
    Format(eitherPaymentHistoryReader, eitherPaymentHistoryWriter)
  }

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
