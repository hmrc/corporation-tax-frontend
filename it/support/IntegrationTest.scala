package support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import IntegrationTest.{wireMockServerHost, wireMockServerPort}

import scala.reflect.ClassTag

trait IntegrationTest extends GuiceOneServerPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Eventually
  with IntegrationPatience {
  this: TestSuite =>

  lazy val wmConfig: WireMockConfiguration = wireMockConfig()
    .port(wireMockServerPort)
    .notifier(new ConsoleNotifier(false)) // Set ConsoleNotifier constructor argument to "true" for additional logging data

  lazy val wireMockServer: WireMockServer = new WireMockServer(wmConfig)

  final override lazy val app:Application =
    new GuiceApplicationBuilder()
      .configure(essentialConfigs)
      .build()

  def inject[T: ClassTag]: T = app.injector.instanceOf[T]

  final val configuration: Configuration = inject[Configuration]

  final val maxRetries = configuration.get[Int]("play.ws.ahc.maxRequestRetry")

  protected def essentialConfigs: Map[String, String] =
    Map(
      "auditing.enabled" -> "false"
    ) ++ microservices

  final protected def microservices: Map[String, String] =
    Set[String]("ct", "auth", "enrolment-store-proxy", "pay-api", "business-tax-account")
      .flatMap{ microServiceName =>
        val key: String = s"microservice.services.$microServiceName"
        Map(s"$key.host" -> wireMockServerHost, s"$key.port" -> wireMockServerPort.toString)
      }.toMap

  override def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireMockServerHost, wireMockServerPort)
    wireMockServer.start()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.shutdown()
  }
}

object IntegrationTest {
  val wireMockServerHost: String = "localhost"
  val wireMockServerPort: Int = 6002
}
