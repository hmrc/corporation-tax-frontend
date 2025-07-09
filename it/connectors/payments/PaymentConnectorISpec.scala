package connectors.payments

import config.FrontendAppConfig
import models.requests.AuthenticatedRequest
import models.{CtEnrolment, CtUtr, SpjRequestBtaCt}
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import support.IntegrationTest
import support.TestConstants.testCtUtr
import support.TestJsonObjects.testNextUrlSuccess
import support.stubs.StubConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PaymentConnectorISpec extends PlaySpec with IntegrationTest {

  override protected def essentialConfigs: Map[String, String] =
    Map(
      "auditing.enabled" -> "false",
      "paymentsFrontendBase" -> "http://localhost:9050/pay-online"
    ) ++ microservices

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  val connector: PaymentConnector = inject[PaymentConnector]
  val frontendAppConfig: FrontendAppConfig = inject[FrontendAppConfig]

  val paymentConnectorUrl: String = "/pay-api/bta/ct/journey/start"

  // Create test input instance of SpjRequestBtaCt
  val amountInPence: Long = 1000L
  val testDate: String = "2023-09-01"
  val backUrl: String = frontendAppConfig.businessAccountHomeAbsoluteUrl

  val spjRequestBtaCt: SpjRequestBtaCt = SpjRequestBtaCt(amountInPence, backUrl, backUrl, testCtUtr, testDate)

  val expectedNextUrl: String = "https://www.tax.service.gov.uk/pay/12345/choose-a-way-to-pay"

  val paymentsFrontendBaseUrl: String = frontendAppConfig.getUrl("paymentsFrontendBase")

  val expectedFailureNextUrl: String = "http://localhost:9050/pay-online/service-unavailable"

  "PaymentConnector" when {

    "successfully posting a valid Spj request" should {

      "return the correct next url value" in {

        StubConnector.withResponseForCtPayLinkPost(paymentConnectorUrl, spjRequestBtaCt)(OK, Some(testNextUrlSuccess))

        val result: Future[NextUrl] = connector.ctPayLink(spjRequestBtaCt)

        await(result) mustBe NextUrl(expectedNextUrl)

        StubConnector.verifyPostRequest(paymentConnectorUrl, count = 1)
      }
    }

    "handling an error" should {

      "notify the user the service is unavailable on receipt of a bad request" in {

        StubConnector.withResponseForCtPayLinkPost(paymentConnectorUrl, spjRequestBtaCt)(BAD_REQUEST, None)

        val result: Future[NextUrl] = connector.ctPayLink(spjRequestBtaCt)

        await(result) mustBe NextUrl(expectedFailureNextUrl)

        StubConnector.verifyPostRequest(paymentConnectorUrl, count = 1)
      }

      "notify the user the service is unavailable on receipt of an internal server error" in {

        StubConnector.withResponseForCtPayLinkPost(paymentConnectorUrl, spjRequestBtaCt)(INTERNAL_SERVER_ERROR, None)

        val result: Future[NextUrl] = connector.ctPayLink(spjRequestBtaCt)

        await(result) mustBe NextUrl(expectedFailureNextUrl)

        StubConnector.verifyPostRequest(paymentConnectorUrl, count = 1)
      }

      "notify the user the service is unavailable on receipt of a bad gateway error" in {

        StubConnector.withResponseForCtPayLinkPost(paymentConnectorUrl, spjRequestBtaCt)(BAD_GATEWAY, None)

        val result: Future[NextUrl] = connector.ctPayLink(spjRequestBtaCt)

        await(result) mustBe NextUrl(expectedFailureNextUrl)

        StubConnector.verifyPostRequest(paymentConnectorUrl, count = 1)
      }

      "notify the service is unavailable on receipt of malformed response" in {

        StubConnector.withFailedResponseForCtPayLinkPost(paymentConnectorUrl, spjRequestBtaCt)

        val result: Future[NextUrl] = connector.ctPayLink(spjRequestBtaCt)

        await(result) mustBe NextUrl(expectedFailureNextUrl)

        StubConnector.verifyPostRequest(paymentConnectorUrl, count = maxRetries + 1)
      }

    }
  }
}
