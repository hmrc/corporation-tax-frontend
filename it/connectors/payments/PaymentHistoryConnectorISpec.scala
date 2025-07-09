package connectors.payments

import models.{CtEnrolment, CtUtr}
import models.payments.CtPaymentRecord
import models.payments.PaymentStatus.{Invalid, Successful}
import models.requests.AuthenticatedRequest
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import support.IntegrationTest
import support.TestJsonObjects.{testEmptyCtPaymentList, testIncompleteCtPayment, testMultipleEntryCtPaymentList, testSingleEntryCtPaymentList}
import support.stubs.StubConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class PaymentHistoryConnectorISpec extends PlaySpec with IntegrationTest {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  val connector: PaymentHistoryConnector = inject[PaymentHistoryConnector]

  val searchTag: String = "testSearchTag"
  val paymentHistoryConnectorUrl: String = s"/pay-api/v2/payment/search/$searchTag?taxType=corporation-tax&searchScope=BTA"

  val expectedEmptyCtPaymentList: List[CtPaymentRecord] = List()

  val amountInPence_001: Long = 10L

  val ctPayment_001: CtPaymentRecord = CtPaymentRecord(
    "Test reference 001",
    amountInPence_001,
    Successful,
    "Some date 001",
    "Tax type 001")

  val expectedSingleEntryCtPaymentList: List[CtPaymentRecord] = List(ctPayment_001)

  val amountInPence_002: Long = -10L

  val ctPayment_002: CtPaymentRecord = CtPaymentRecord(
    "Test reference 002",
    amountInPence_002,
    Invalid,
    "Some date 002",
    "Tax type 002")

  val amountInPence_003: Long = 2001L

  val ctPayment_003: CtPaymentRecord = CtPaymentRecord(
    "Test reference 003",
    amountInPence_003,
    Successful,
    "Some date 003",
    "Tax type 003"
  )

  val expectedMultipleEntryCtPaymentList: List[CtPaymentRecord] =
  List(ctPayment_001, ctPayment_002, ctPayment_003)

  val expectedErrorMsgForHttpError: String = "couldn't handle response from payment api"

  "PaymentHistoryConnector" when {

    "requesting Ct payments" should {

      "return an empty list of payments when the response indicates no payments have been made" in {

        StubConnector.withResponse(paymentHistoryConnectorUrl)(OK, Some(testEmptyCtPaymentList))

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(ctPayments) => ctPayments mustBe expectedEmptyCtPaymentList
          case Left(error) => fail(s"Attempt to retrieve empty list of Ct payments failed with error : $error")
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = 1)
      }

      "return a single entry list of Ct payments when the response defines a single payment only" in {

        StubConnector.withResponse(paymentHistoryConnectorUrl)(OK, Some(testSingleEntryCtPaymentList))

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(ctPayments) => ctPayments mustBe expectedSingleEntryCtPaymentList
          case Left(error) => fail(s"Attempt to retrieve single entry Ct payment list failed with error : $error")
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = 1)
      }

      "return a multiple entry list of Ct payments when the response defines multiple payments" in {

        StubConnector.withResponse(paymentHistoryConnectorUrl)(OK, Some(testMultipleEntryCtPaymentList))

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(ctPayments) => ctPayments mustBe expectedMultipleEntryCtPaymentList
          case Left(error) => fail(s"Attempt to retrieve multiple entry Ct payment list failed with error : $error")
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = 1)
      }

    }

    "error handling" should {

      "manage a Ct payments list with an incomplete payment" in {

        StubConnector.withResponse(paymentHistoryConnectorUrl)(OK, Some(testIncompleteCtPayment))

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(_) => fail("Connector should not create list of Ct payments from incomplete JSON")
          case Left(errMsg) => errMsg mustBe "unable to parse data from payment api"
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = 1)
      }

      "manage a response status of bad request" in {

        StubConnector.withResponse(paymentHistoryConnectorUrl)(BAD_REQUEST, None)

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(_) => fail("Connector should not return a list of Ct payments for a response with a status of bad request")
          case Left(errMsg) => errMsg mustBe expectedErrorMsgForHttpError
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = 1)
      }

      "manage a response status of not found" in {

        StubConnector.withResponse(paymentHistoryConnectorUrl)(NOT_FOUND, None)

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(_) => fail("Connector should not return a list of Ct payments for a response with a status of not found")
          case Left(errMsg) => errMsg mustBe expectedErrorMsgForHttpError
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = 1)
      }

      "manage a response status of internal server error" in {

        StubConnector.withResponse(paymentHistoryConnectorUrl)(INTERNAL_SERVER_ERROR, None)

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(_) => fail("Connector should not return a list of Ct payments for a response with a status of internal server error")
          case Left(errMsg) => errMsg mustBe expectedErrorMsgForHttpError
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = 1)
      }

      "manage a malformed response" in {

        StubConnector.withFailedResponse(paymentHistoryConnectorUrl)

        val result: Future[Either[String, List[CtPaymentRecord]]] = connector.get(searchTag)

        await(result) match {
          case Right(_) => fail("Connector should not return a list of Ct payments for a malformed response")
          case Left(errMsg) => errMsg mustBe "exception thrown from payment api"
        }

        StubConnector.verifyRequest(paymentHistoryConnectorUrl, count = maxRetries + 1)
      }
    }
  }

}
