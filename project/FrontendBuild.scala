import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion


object FrontendBuild extends Build with MicroService {

  val appName = "corporation-tax-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "3.14.0-play-25"
  private val logbackJsonLoggerVersion = "4.6.0"
  private val govukTemplateVersion = "5.36.0-play-25"
  private val playUiVersion = "8.7.0-play-25"
  private val scalaTestVersion = "3.0.4"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val pegdownVersion = "1.6.0"
  private val mockitoAllVersion = "1.10.19"
  private val httpCachingClientVersion = "8.4.0-play-25"
  private val playReactivemongoVersion = "6.8.0"
  private val playConditionalFormMappingVersion = "1.2.0-play-25"
  private val playLanguageVersion = "3.4.0"
  private val bootstrapVersion = "4.13.0"
  private val scalacheckVersion = "1.13.4"
  private val playPartialsVersion = "6.9.0-play-25"
  private val domainVersion = "5.6.0-play-25"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc" %% "bootstrap-play-25" % bootstrapVersion,
    "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % "1.10.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
