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

package views.components

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.twirl.api.Html
import views.html.components.heading

class HeadingSpec extends PlaySpec with GuiceOneAppPerSuite {

  implicit lazy val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq.empty)

  val view : Html = heading("test-id", "Test heading")

  "Heading component" must {

    "include heading with heading text and  class" in {
      val doc: Document = Jsoup.parse(view.toString)

      val forms = doc.select("h1")
      forms.size mustBe 1

      forms.get(0).attr("id") mustBe "test-id"
      forms.get(0).text() mustBe "Test heading"
      forms.get(0).className() mustBe "govuk-heading-l govuk-!-margin-top-0"
    }
  }
}
