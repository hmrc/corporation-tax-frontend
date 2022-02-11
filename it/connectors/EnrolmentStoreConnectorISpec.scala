package connectors

import models.{UserEnrolmentStatus, UserEnrolments}
import org.joda.time.LocalDateTime
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import support.DateTimeUtils.createLocalDateTime
import support.IntegrationTest
import support.TestConstants._
import support.TestJsonObjects.{testInvalidUserEnrolments, testMultipleUserEnrolments, testSingleUserEnrolment}
import support.stubs.StubConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EnrolmentStoreConnectorISpec extends PlaySpec with IntegrationTest {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val connector: EnrolmentStoreConnector = inject[EnrolmentStoreConnector]

  val enrolmentStoreProxyUrl: String = s"/enrolment-store/users/$testCtUtr/enrolments?service=IR-CT"

  val testService1Id: String = "test-service-1"
  val testServiceId2: String = "test-service-2"
  val testServiceId3: String = "test-service-3"

  val serviceStatusActive: String = "active"
  val serviceStatusInactive: String = "inactive"

  val testService1EnrolmentTokenExpiryDate: LocalDateTime = createLocalDateTime("2028-01-01T12:00:00.000")
  val testService2EnrolmentTokenExpiryDate: LocalDateTime = createLocalDateTime("2028-01-02T23:59:59.999")
  val testService3EnrolmentTokenExpiryDate: LocalDateTime = createLocalDateTime("2028-01-03T00:00:00.000")

  val testService1EnrolmentStatus: UserEnrolmentStatus = UserEnrolmentStatus(
    testService1Id,
    Some(serviceStatusActive),
    Some(testService1EnrolmentTokenExpiryDate))

  val expectedSingleUserEnrolment: UserEnrolments = UserEnrolments(List(testService1EnrolmentStatus))

  val testService2EnrolmentStatus: UserEnrolmentStatus = UserEnrolmentStatus(
    testServiceId2,
    Some(serviceStatusInactive),
    Some(testService2EnrolmentTokenExpiryDate)
  )

  val testService3EnrolmentStatus: UserEnrolmentStatus = UserEnrolmentStatus(
    testServiceId3,
    Some(serviceStatusActive),
    Some(testService3EnrolmentTokenExpiryDate)
  )

  val expectedUserMultipleEnrolments: UserEnrolments = UserEnrolments(
    List(testService1EnrolmentStatus, testService2EnrolmentStatus, testService3EnrolmentStatus)
  )

  "EnrolmentStoreConnector" when {

    "Requesting user enrolments" should {

      "return a single enrolment for a valid JSON response with a single enrolment" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(OK, Some(testSingleUserEnrolment))

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(userEnrolments) => userEnrolments mustBe expectedSingleUserEnrolment
          case Left(error) => fail(s"Attempt to retrieve single enrolment for user failed with error : $error")
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "return multiple enrolments for a valid JSON response with multiple enrolments" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(OK, Some(testMultipleUserEnrolments))

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(userEnrolments) => userEnrolments mustBe expectedUserMultipleEnrolments
          case Left(error) => fail(s"Attempt to retrieve multiple enrolments for user failed with error : $error")
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "return an error when the JSON in a response is invalid" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(OK, Some(testInvalidUserEnrolments))

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of UserEnrolments should not be created from an invalid response")
          case Left(errMsg) => errMsg mustBe "Unable to parse data from enrolment API"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

    }

    "Enrolment connector error handling" should {

      "handle a NOT_FOUND response" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(NOT_FOUND, None)

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of UserEnrolments should not be created for a response of NOT_FOUND")
          case Left(errMsg) => errMsg mustBe "User not found from enrolment API"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "handle a BAD_REQUEST response" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(BAD_REQUEST, None)

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of UserEnrolments should not be created for a response of BAD_REQUEST")
          case Left(errMsg) => errMsg mustBe "Bad request to enrolment API"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "handle a FORBIDDEN response" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(FORBIDDEN, None)

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of UserEnrolments should not be created for a response of FORBIDDEN")
          case Left(errMsg) => errMsg mustBe "Forbidden from enrolment API"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "handle a SERVICE_UNAVAILABLE response" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(SERVICE_UNAVAILABLE, None)

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of UserEnrolments should not be created for a response of SERVICE_UNAVAILABLE")
          case Left(errMsg) => errMsg mustBe "Unexpected error from enrolment API"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "handle a NO_CONTENT response" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(NO_CONTENT, None)

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of UserEnrolments should not be created for a response of NO_CONTENT")
          case Left(errMsg) => errMsg mustBe "No content from enrolment API"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "handle an unexpected response status" in {

        StubConnector.withResponse(enrolmentStoreProxyUrl)(NETWORK_AUTHENTICATION_REQUIRED, None)

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of UserEnrolments should not be created for a response of NETWORK_AUTHENTICATION_REQUIRED")
          case Left(errMsg) => errMsg mustBe "Enrolment API couldn't handle response code"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = 1)
      }

      "handle a failed response" in {

        StubConnector.withFailedResponse(enrolmentStoreProxyUrl)

        val result: Future[Either[String,UserEnrolments]] = connector.getEnrolments(testCtUtr)

        await(result) match {
          case Right(_) => fail("An instance of user enrolments should not be created for a failed response")
          case Left(errMsg) => errMsg mustBe "Exception thrown from enrolment API"
        }

        StubConnector.verifyRequest(enrolmentStoreProxyUrl, count = maxRetries + 1)
      }

    }
  }

}
