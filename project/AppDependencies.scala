import sbt.*

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport.*

  val bootstrapVersion: String = "9.16.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc" %% "http-caching-client-play-30"  % "12.2.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30"   % "11.13.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq()
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq("org.mockito" % "mockito-core" % "5.14.2" % scope)
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test: Seq[ModuleID] = Seq(
        "com.github.tomakehurst"        % "wiremock"              % "2.33.2"  % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.18.0"  % scope)
    }.test
  }

  object TestCommon {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "test,it"
      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatestplus.play"  %% "scalatestplus-play"     % "7.0.1"             % scope,
        "org.playframework"       %% "play-test"              % PlayVersion.current % scope,
        "org.jsoup"               % "jsoup"                   % "1.18.3"            % scope,
        "uk.gov.hmrc"             %% "bootstrap-test-play-30" % bootstrapVersion    % scope,
        "org.scalatestplus"       %% "mockito-5-10"           % "3.2.18.0"          % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest() ++ TestCommon()
}
