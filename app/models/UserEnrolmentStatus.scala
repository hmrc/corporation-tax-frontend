/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.libs.json._

import scala.util.Try


case class UserEnrolmentStatus(service: String,
                               state: Option[String],
                               enrolmentTokenExpiryDate: Option[LocalDateTime])

object UserEnrolmentStatus {

  val dateFormat: String = "yyyy-MM-dd HH:mm:ss.SSS"

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat)

  implicit def enrolmentTokenExpiryDateWrites: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    override def writes(localDateTime: LocalDateTime): JsValue = JsString(localDateTime.toString)
  }

  implicit def enrolmentTokenExpiryDateReads: Reads[LocalDateTime] = new Reads[LocalDateTime] {
    override def reads(json: JsValue): JsResult[LocalDateTime] =
        Try(JsSuccess(LocalDateTime.parse(json.as[String], dateFormatter), JsPath)).getOrElse(JsError())
  }

  implicit val format: OFormat[UserEnrolmentStatus] = Json.format[UserEnrolmentStatus]
}
