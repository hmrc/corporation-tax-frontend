package connectors

import play.twirl.api.Html
import support.IntegrationTest
import support.TestHtmlObjects.testServiceInfoPartial
import support.stubs.StubConnector
import org.scalatest.{MustMatchers, WordSpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class ServiceInfoPartialConnectorISpec extends WordSpec with MustMatchers with IntegrationTest {

  implicit val headerCarrierForPartials: HeaderCarrierForPartials = HeaderCarrierForPartials(hc = HeaderCarrier())

  lazy val connector: ServiceInfoPartialConnector = inject[ServiceInfoPartialConnector]

  val serviceInfoPartialUrl: String = "/business-account/partial/service-info"

  val expectedSuccessHtml: Html = Html(testServiceInfoPartial)
  val expectedFailureHtml: Html = Html("")

  "ServiceInfoPartialConnector" when {

    "handling valid responses" should {

      "return an instance of Html containing the partial Html from the response" in {

        StubConnector.withHtmlPartialResponse(serviceInfoPartialUrl)(OK, Some(testServiceInfoPartial))

        val result: Future[Html] = connector.getServiceInfoPartial()

        await(result) mustBe expectedSuccessHtml

        StubConnector.verifyRequest(serviceInfoPartialUrl, count = 1)
      }
    }

    "handling responses with error" should {

      "return an empty instance of Html when an internal server error is raised" in {

        StubConnector.withHtmlPartialResponse(serviceInfoPartialUrl)(INTERNAL_SERVER_ERROR, None)

        val result: Future[Html] = connector.getServiceInfoPartial()

        await(result) mustBe expectedFailureHtml

        StubConnector.verifyRequest(serviceInfoPartialUrl, count = 1)
      }

      "return an empty instance of Html for a corrupted response" in {

        StubConnector.withFailedResponse(serviceInfoPartialUrl)

        val result: Future[Html] = connector.getServiceInfoPartial()

        await(result) mustBe expectedFailureHtml

        StubConnector.verifyRequest(serviceInfoPartialUrl, count = maxRetries + 1)
      }
    }
  }

}
