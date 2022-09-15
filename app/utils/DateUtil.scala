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

package utils


import models.requests.{AuthenticatedRequest, ServiceInfoRequest}
import play.api.libs.json.{JsError, JsSuccess, Reads}
import play.api.mvc.AnyContent

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.util.{Failure, Success, Try}

class DateUtil extends LoggingUtil {

  implicit def offsetDateTimeFromLocalDateTimeFormatReads()(implicit request: AuthenticatedRequest[_]): Reads[OffsetDateTime] = { json =>
    json.as[String].parseOffsetDateTimeFromLocalDateTimeFormat() match {
      case Right(value) => JsSuccess(value)
      case Left(error) => JsError("not a valid date " + error.errorMessage)
    }
  }

  implicit class LocalDateTimeExtensions(localDateTime: LocalDateTime) {
    def utcOffset: OffsetDateTime = localDateTime.atOffset(ZoneOffset.UTC)
  }

  implicit class StringExtensions(string: String) {

    def parseOffsetDateTimeFromLocalDateTimeFormat(formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME)(implicit request: AuthenticatedRequest[_])
                                                  : Either[DateError, OffsetDateTime] = {

      Try(LocalDateTime.parse(string, formatter).utcOffset) match {
        case Success(offsetDateTime) => Right(offsetDateTime)
        case Failure(exception) =>
          warnLog(s"[DateUtil][parseOffsetDateTimeFromLocalDateTimeFormat] could not parse createdOn dateTime" +
            s"\n createdOn: $string" +
            s"\n exception: ${exception.getMessage}"
          )
          Left(DateParseError(exception.getMessage))
      }
    }
  }

  sealed trait DateError {
    val errorMessage: String
  }
  case class DateParseError(e: String) extends DateError {
    val errorMessage: String = e
  }
}
