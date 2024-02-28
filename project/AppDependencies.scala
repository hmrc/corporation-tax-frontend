import sbt._

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val bootstrapVersion = "7.12.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc" %% "http-caching-client" % "10.0.0-play-28",
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-28" % "8.5.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq()
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq("org.mockito" % "mockito-core" % "4.11.0" % scope)
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test: Seq[ModuleID] = Seq(
        "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0" % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.14.1" % scope)
    }.test
  }

  object TestCommon {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "test,it"
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.jsoup" % "jsoup" % "1.15.3" % scope,
        "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion  % scope,
        "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest() ++ TestCommon()
}
