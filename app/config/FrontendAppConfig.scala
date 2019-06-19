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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import models.CtEnrolment
import play.api.Mode.Mode
import play.api.i18n.Lang
import play.api.mvc.{Call, Request}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig
import utils.PortalUrlBuilder

@Singleton
class FrontendAppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig with PortalUrlBuilder {

  override protected def mode: Mode = environment.mode

  private def loadConfig(key: String): String = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private lazy val contactHost: String = runModeConfiguration.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier: String = "corporationtaxfrontend"

  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrl: String = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl: String = s"$contactHost/contact/beta-feedback-unauthenticated"

  lazy val authUrl: String = baseUrl("auth")
  lazy val btaUrl: String = baseUrl("business-tax-account")
  lazy val ctUrl: String = baseUrl("ct")

  lazy val loginUrl: String = loadConfig("urls.login")
  lazy val loginContinueUrl: String = loadConfig("urls.loginContinue")

  def enrolmentStoreUrl: String = baseUrl("enrolment-store-proxy")

  def payApiUrl: String = baseUrl("pay-api")

  private lazy val businessAccountHost: String = runModeConfiguration.getString("urls.business-account.host").getOrElse("")
  private lazy val helpAndContactHost: String = runModeConfiguration.getString("urls.help-and-contact.host").getOrElse("")
  lazy val businessAccountHome: String = businessAccountHost + "/business-account"

  lazy val businessAccountHomeAbsoluteUrl: String = getUrl("businessAccountAuthority") + "/business-account"

  private lazy val portalHost: String = loadConfig(s"urls.external.portal.host")

  def getUrl(key: String): String = loadConfig(s"urls.$key")

  def getGovUrl(key: String): String = loadConfig(s"urls.external.govuk.$key")

  def getFormsUrl(key: String): String = loadConfig(s"urls.forms.$key")

  def getBusinessAccountUrl(key: String): String = businessAccountHost + loadConfig(s"urls.business-account.$key")

  def getPortalUrl(key: String)(ctEnrolment: CtEnrolment)(implicit request: Request[_]): String =
    buildPortalUrl(portalHost + loadConfig(s"urls.external.portal.$key"))(ctEnrolment)

  def getHelpAndContactUrl(key: String): String = helpAndContactHost + runModeConfiguration.getString(s"urls.help-and-contact.$key").getOrElse("")

  lazy val languageTranslationEnabled: Boolean = runModeConfiguration.getBoolean("toggles.welsh-translation").getOrElse(true)

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val googleTagManagerId: String = loadConfig(s"google-tag-manager.id")

  def sessionTimeoutInSeconds: Long = 900

  def sessionCountdownInSeconds: Int = 60

  lazy val getCTPaymentHistoryToggle: Boolean = runModeConfiguration.getBoolean("toggles.ct-payment-history").getOrElse(false)

}
