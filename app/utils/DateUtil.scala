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

package utils


import play.api.libs.json.{JsError, JsSuccess, Reads}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.util.{Failure, Success, Try}

trait DateUtil {

  implicit val offsetDateTimeFromLocalDateTimeFormatReads: Reads[OffsetDateTime] = { json =>
    json.as[String].parseOffsetDateTimeFromLocalDateTimeFormat() match {
      case Right(value) => JsSuccess(value)
      case Left(error) => JsError("not a valid date " + error.errorMessage)
    }
  }

  implicit class LocalDateTimeExtensions(localDateTime: LocalDateTime) {
    def utcOffset: OffsetDateTime = localDateTime.atOffset(ZoneOffset.UTC)
  }

   implicit class StringExtensions(string: String) {

    def parseOffsetDateTimeFromLocalDateTimeFormat(formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME)
                                                  : Either[DateError, OffsetDateTime] = {

      Try(LocalDateTime.parse(string, formatter).utcOffset) match {
        case Success(offsetDateTime) => Right(offsetDateTime)
        case Failure(exception) => Left(DateParseError(exception.getMessage, string))
      }
    }
  }

  sealed trait DateError {
    val errorMessage: String
    val dateFailedToParse: String
  }
  case class DateParseError(e: String, dateFailed: String) extends DateError {
    val dateFailedToParse: String = dateFailed
    val errorMessage: String = e
  }
}
