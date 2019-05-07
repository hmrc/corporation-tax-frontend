/*
 * Copyright 2019 HM Revenue & Customs
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

package forms.behaviours

import play.api.data.Form
import forms.FormSpec
import models._

trait FormBehaviours extends FormSpec {

  val validData: Map[String, String]

  val form: Form[_]

  def questionForm[A](expectedResult: A) = {
    "bind valid values correctly" in {
      val boundForm = form.bind(validData)
      boundForm.get shouldBe expectedResult
    }
  }

  def formWithOptionField(field: Field, validValues: String*) = {
    for (validValue <- validValues) {
      s"bind when ${field.name} is set to $validValue" in {
        val data = validData + (field.name -> validValue)
        val boundForm = form.bind(data)
        boundForm.errors.isEmpty shouldBe true
      }
    }

    s"fail to bind when ${field.name} is omitted" in {
      val data = validData - field.name
      val expectedError = error(field.name, field.errorKeys(Required))
      checkForError(form, data, expectedError)
    }

    s"fail to bind when ${field.name} is invalid" in {
      val data = validData + (field.name -> "invalid value")
      val expectedError = error(field.name, field.errorKeys(Invalid))
      checkForError(form, data, expectedError)
    }
  }
}
