/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.domain.CtUtr
class PortalUrlBuilderSpec extends SpecBase {

  object PortalUrlBuilder extends PortalUrlBuilder

  val enrolment = Some(CtEnrolment(CtUtr("a-users-utr"), isActivated = true))

  "build portal url" when {
    "there is nothing to replace" should {
      "return the provided url with no replacements" in {
        PortalUrlBuilder.buildPortalUrl("http://testurl")(enrolment) mustBe "http://testurl"
      }
    }
    "there is <utr>" should {
      "return the provided url with the current users UTR" in {
        PortalUrlBuilder.buildPortalUrl("http://testurl/<utr>/")(enrolment) mustBe "http://testurl/a-users-utr/"
      }
    }
  }
}
