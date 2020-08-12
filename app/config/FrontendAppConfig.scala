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

package config

import controllers.routes
import javax.inject.{Inject, Singleton}
import models.CtEnrolment
import play.api.i18n.Lang
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.language.LanguageUtils
import utils.PortalUrlBuilder

@Singleton
class FrontendAppConfig @Inject()(config: ServicesConfig,
                                  val languageUtils: LanguageUtils) extends PortalUrlBuilder {

  private def loadConfig(key: String): String = config.getString(key)

  private lazy val contactHost: String = config.getString("contact-frontend.host")
  private val contactFormServiceIdentifier: String = "corporationtaxfrontend"

  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val btaUrl: String = config.baseUrl("business-tax-account")
  lazy val ctUrl: String = config.baseUrl("ct")

  lazy val loginUrl: String = loadConfig("urls.login")
  lazy val loginContinueUrl: String = loadConfig("urls.loginContinue")

  def enrolmentStoreUrl: String = config.baseUrl("enrolment-store-proxy")

  def payApiUrl: String = config.baseUrl("pay-api")

  private lazy val businessAccountHost: String = config.getString("urls.business-account.host")
  private lazy val helpAndContactHost: String = config.getString("urls.help-and-contact.host")
  lazy val businessAccountHome: String = businessAccountHost + "/business-account"

  lazy val businessAccountHomeAbsoluteUrl: String = getUrl("businessAccountAuthority") + "/business-account"

  private lazy val portalHost: String = loadConfig(s"urls.external.portal.host")

  def getUrl(key: String): String = loadConfig(s"urls.$key")

  def getGovUrl(key: String): String = loadConfig(s"urls.external.govuk.$key")

  def getFormsUrl(key: String): String = loadConfig(s"urls.forms.$key")

  def getBusinessAccountUrl(key: String): String = businessAccountHost + loadConfig(s"urls.business-account.$key")

  def getPortalUrl(key: String)(ctEnrolment: CtEnrolment)(implicit request: Request[_]): String =
    buildPortalUrl(portalHost + loadConfig(s"urls.external.portal.$key"))(ctEnrolment)

  def getHelpAndContactUrl(key: String): String = helpAndContactHost + config.getString(s"urls.help-and-contact.$key")

  lazy val languageTranslationEnabled: Boolean = config.getBoolean("toggles.welsh-translation")

  def languageMap: Map[String, Lang] = Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage(lang: String): Call = routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val googleTagManagerId: String = loadConfig(s"google-tag-manager.id")

  val sessionTimeoutInSeconds: Long = 900
  val sessionCountdownInSeconds: Int = 60

}
