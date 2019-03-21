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

package utils

import base.SpecBase
import models.CtEnrolment
import play.api.mvc.Cookie
import uk.gov.hmrc.domain.CtUtr
class PortalUrlBuilderSpec extends SpecBase {

  class TestUrlBuilder(override val languageHelper: LanguageHelper) extends PortalUrlBuilder

  val languageHelper = app.injector.instanceOf[LanguageHelper]
  val portalUrlBuilder = new TestUrlBuilder(languageHelper)

  val enrolment = CtEnrolment(CtUtr("a-users-utr"), isActivated = true)

  val fakeRequestWithWelsh = fakeRequest.withCookies(Cookie("PLAY_LANG", "cy"))

  "build portal url" when {
    "there is <utr>" should {
      "return the provided url with the current users UTR" in {
        portalUrlBuilder.buildPortalUrl("http://testurl/<utr>/")(enrolment)(fakeRequest) mustBe "http://testurl/a-users-utr/?lang=eng"
      }
    }

    "the user is in english" should {
      "append ?lang=eng to given url" in {
        portalUrlBuilder.buildPortalUrl("http://testurl")(enrolment)(fakeRequest) mustBe "http://testurl?lang=eng"
      }
    }

    "the user is in welsh" should {
      "append ?lang=cym to given url" in {
        portalUrlBuilder.buildPortalUrl("http://testurl")(enrolment)(fakeRequestWithWelsh) mustBe "http://testurl?lang=cym"
      }
    }
  }
}
