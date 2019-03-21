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
import play.api.Configuration
import play.api.i18n.Lang
import controllers.routes
import models.CtEnrolment
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}
import utils.{LanguageHelper, PortalUrlBuilder}

@Singleton
class FrontendAppConfig @Inject()(val configuration: Configuration, runMode: RunMode, override val languageHelper: LanguageHelper) extends
  ServicesConfig(configuration, runMode) with PortalUrlBuilder {


  private def loadConfig(key: String) = configuration.get[String](key)

  private lazy val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "corporationtaxfrontend"

  lazy val analyticsToken = loadConfig(s"google-analytics.token")
  lazy val analyticsHost = loadConfig(s"google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"

  lazy val authUrl = baseUrl("auth")
  lazy val btaUrl = baseUrl("business-tax-account")
  lazy val ctUrl = baseUrl("ct")

  lazy val loginUrl = loadConfig("urls.login")
  lazy val loginContinueUrl = loadConfig("urls.loginContinue")

  private lazy val businessAccountHost = configuration.get[String]("urls.business-account.host")
  private lazy val helpAndContactHost = configuration.get[String]("urls.help-and-contact.host")
  lazy val businessAccountHome = businessAccountHost + "/business-account"

  private lazy val portalHost = loadConfig(s"urls.external.portal.host")

  def getUrl(key: String): String = loadConfig(s"urls.$key")
  def getGovUrl(key: String): String = loadConfig(s"urls.external.govuk.$key")
  def getFormsUrl(key: String): String = loadConfig(s"urls.forms.$key")
  def getBusinessAccountUrl(key: String): String = businessAccountHost + loadConfig(s"urls.business-account.$key")
  def getPortalUrl(key: String)(ctEnrolment: CtEnrolment)(implicit request: Request[_]): String =
    buildPortalUrl(portalHost + loadConfig(s"urls.external.portal.$key"))(ctEnrolment)
  def getHelpAndContactUrl(key: String): String = helpAndContactHost + configuration.get[String](s"urls.help-and-contact.$key")

  lazy val languageTranslationEnabled = configuration.get[Boolean]("microservice.services.features.welsh-translation")
  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))
  def routeToSwitchLanguage = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)
  lazy val googleTagManagerId = loadConfig(s"google-tag-manager.id")
}
