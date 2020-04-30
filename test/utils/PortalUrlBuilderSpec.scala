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

package utils

import base.SpecBase
import models.CtEnrolment
import play.api.mvc.{AnyContent, Cookie}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.play.language.LanguageUtils

class PortalUrlBuilderSpec extends SpecBase {

  object TestPortalUrlBuilder extends PortalUrlBuilder {
    val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  }

  val enrolment: CtEnrolment = CtEnrolment(CtUtr("a-users-utr"), isActivated = true)

  val fakeRequestWithWelsh: FakeRequest[AnyContent] = fakeRequest.withCookies(Cookie("PLAY_LANG", "cy"))

  "build portal url" when {
    "there is <utr>" should {
      "return the provided url with the current users UTR" in {
        TestPortalUrlBuilder.buildPortalUrl("http://testurl/<utr>/")(enrolment)(fakeRequest) mustBe "http://testurl/a-users-utr/?lang=eng"
      }
    }

    "the user is in english" should {
      "append ?lang=eng to given url" in {
        TestPortalUrlBuilder.buildPortalUrl("http://testurl")(enrolment)(fakeRequest) mustBe "http://testurl?lang=eng"
      }
    }

    "the user is in welsh" should {
      "append ?lang=cym to given url" in {
        TestPortalUrlBuilder.buildPortalUrl("http://testurl")(enrolment)(fakeRequestWithWelsh) mustBe "http://testurl?lang=cym"
      }
    }
  }
}
