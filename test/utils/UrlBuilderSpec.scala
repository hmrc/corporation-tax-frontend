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

import base.SpecBase
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class UrlBuilderSpec extends SpecBase {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "build url processes a URL template" should {

    "replace multiple different tokens" in {

      val urlToReplaceWithValues = "http://someserver:8080/something1/<keyToReplace1>/something2/<keyToReplace2>"

      val sequenceToReplace = Seq("<keyToReplace1>" -> Some("value1"), "<keyToReplace2>" -> Some("value2"))

      val builtUrl = UrlBuilder.buildUrl(urlToReplaceWithValues, sequenceToReplace)

      builtUrl mustBe "http://someserver:8080/something1/value1/something2/value2"
    }


    "replace Some token and not None" in {

      val urlToReplaceWithValues = "http://someserver:8080/something1/<keyToReplace1>/something2/<keyToReplace2>"

      val sequenceToReplace = Seq("<keyToReplace1>" -> Some("value1"), "<keyToReplace2>" -> None)

      val builtUrl = UrlBuilder.buildUrl(urlToReplaceWithValues, sequenceToReplace)

      builtUrl mustBe "http://someserver:8080/something1/value1/something2/<keyToReplace2>"
    }

    "replace the same token multiple times" in {

      val urlToReplaceWithValues = "http://someserver:8080/something1/<keyToReplace1>/something2/<keyToReplace1>"

      val sequenceToReplace = Seq("<keyToReplace1>" -> Some("value1"))

      val builtUrl = UrlBuilder.buildUrl(urlToReplaceWithValues, sequenceToReplace)

      builtUrl mustBe "http://someserver:8080/something1/value1/something2/value1"
    }
  }

}
