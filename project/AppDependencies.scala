import sbt._

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.2.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "4.9.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.61.0-play-27",
    "uk.gov.hmrc" %% "play-health" % "3.16.0-play-27",
    "uk.gov.hmrc" %% "play-ui" % "8.21.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.4.0-play-27",
    "uk.gov.hmrc" %% "play-language" % "4.13.0-play-27",
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-27"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq()
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq("org.mockito" % "mockito-core" % "3.7.7" % scope)
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test: Seq[ModuleID] = Seq("com.github.tomakehurst" % "wiremock-jre8" % "2.28.0" % scope)
    }.test
  }

  object TestCommon {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "test,it"
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.13.1" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest() ++ TestCommon()
}
