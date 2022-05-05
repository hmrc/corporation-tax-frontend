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

import base.SpecBase
import play.api.libs.json.{JsError, JsResult, JsValue, Json}

import java.time.{OffsetDateTime, ZoneOffset}

class DateUtilSpec extends SpecBase with DateUtil {

  val offsetDateTime: OffsetDateTime = OffsetDateTime.of(2022,4,1,12,0,0,0,ZoneOffset.UTC)

  "offsetDateTimeFromLocalDateTimeFormatReads" when {

    "string is of LocalDateTime format" must {

      "return OffsetDateTime" in {

        val localDateTimeJson: JsValue = Json.toJson("2022-04-01T12:00:00.000")

        localDateTimeJson.as[OffsetDateTime] mustBe offsetDateTime
      }
    }

    "string is of OffsetDateTime format" must {

      "return OffsetDateTime" in {

        val offsetDateTimeJson: JsValue = Json.toJson("2022-04-01T12:00Z")

        offsetDateTimeJson.as[OffsetDateTime] mustBe offsetDateTime
      }
    }

    "string is of OffsetDateTime format" must {

      "fail to parse string" in {

        val notADateJson: JsValue = Json.toJson("This is not a date")

        val result: JsResult[OffsetDateTime] = notADateJson.validate[OffsetDateTime]

        result.isInstanceOf[JsError] mustBe true
        result.leftSideValue.toString.contains("not a valid date Text 'This is not a date' could not be parsed at index 0") mustBe true
      }
    }
  }

  "parseOffsetDateTimeFromLocalDateTimeFormat" when {

    "string is of LocalDateTime format" must {

      "return Right(OffsetDateTime)" in {

        val string: String = "2022-04-01T12:00:00.000"

        string.parseOffsetDateTimeFromLocalDateTimeFormat() mustBe Right(offsetDateTime)
      }
    }

    "string is of OffsetDateTime format" must {

      "return Right(OffsetDateTime)" in {

        val string: String = "2022-04-01T12:00Z"

        string.parseOffsetDateTimeFromLocalDateTimeFormat() mustBe Right(offsetDateTime)
      }
    }

    "string is of is not a date time" must {

      "return Right(OffsetDateTime)" in {

        val string: String = "This is not a date"

        string.parseOffsetDateTimeFromLocalDateTimeFormat() mustBe
          Left(DateParseError("Text 'This is not a date' could not be parsed at index 0"))
      }
    }
  }
}
