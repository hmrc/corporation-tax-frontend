import sbt._

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.20.0",
    "uk.gov.hmrc" %% "http-caching-client" % "9.4.0-play-28",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "2.0.0-play-28"
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
      override lazy val test: Seq[ModuleID] = Seq("com.github.tomakehurst" % "wiremock-jre8" % "2.27.2" % scope)
    }.test
  }

  object TestCommon {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "test,it"
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.jsoup" % "jsoup" % "1.14.3" % scope,
        "uk.gov.hmrc" %% "bootstrap-test-play-28" % "5.20.0"  % scope,
        "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest() ++ TestCommon()
}
