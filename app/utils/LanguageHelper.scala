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

import javax.inject.Singleton

import com.google.inject.ImplementedBy
import play.api.Play
import play.api.i18n.Lang
import play.api.mvc.{Flash, RequestHeader}

object LanguageHelper{
  val EnglishLangCode = "en"
  val WelshLangCode = "cy"

  val English = Lang(EnglishLangCode)
  val Welsh = Lang(WelshLangCode)
}

@ImplementedBy(classOf[LanguageHelperImpl])
trait LanguageHelper {
  def getCurrentLang(implicit request: RequestHeader):Lang

  val SwitchIndicatorKey = "switching-language"
  val FlashWithSwitchIndicator = Flash(Map(SwitchIndicatorKey -> "true"))
}

@Singleton
class LanguageHelperImpl extends LanguageHelper{
  private val SelectedLanguageCookieName = "PLAY_LANG"
  def getCurrentLang(implicit request: RequestHeader): Lang = {
    request.cookies.get(SelectedLanguageCookieName).map { cookie =>
      if (cookie.value == LanguageHelper.Welsh.code) LanguageHelper.Welsh else LanguageHelper.English
    }.getOrElse(LanguageHelper.English)
  }
}
