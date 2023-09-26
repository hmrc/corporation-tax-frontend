package connectors

import models._
import models.requests.AuthenticatedRequest
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import support.IntegrationTest
import support.TestConstants._
import support.TestJsonObjects._
import support.stubs.StubConnector
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CtConnectorISpec extends PlaySpec with IntegrationTest {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  lazy val connector: CtConnector = inject[CtConnector]

  val accountSummaryUrl: String = s"/ct/$testCtUtr/account-summary"

  val designatoryDetailsUrl: String = s"/ct/$testCtUtr/designatory-details"

  val firstName: String = "testUserFirstName"
  val surname: String = "testUserSurname"

  val companyAddressLine1: String = "Test company address line 1"
  val companyAddressLine2: String = "Test company address line 2"
  val companyAddressLine3: String = "Test company address line 3"
  val companyAddressLine4: String = "Test company address line 4"
  val companyAddressLine5: String = "Test company address line 5"
  val companyPostcode: String = "XX10 1XX"
  val companyForeignCountry: String = "N"

  val communicationAddressLine1: String = "Test communication address line 1"
  val communicationAddressLine2: String = "Test communication address line 2"
  val communicationAddressLine3: String = "Test communication address line 3"
  val communicationAddressLine4: String = "Test communication address line 4"
  val communicationAddressLine5: String = "Test communication address line 5"
  val communicationPostcode: String = "YY10 1YY"
  val communicationForeignCountry: String = "Y"

  val companyPhoneNo: String = "00000 000000"
  val companyEmail: String = "companyTestUser@test.com"

  val communicationPhoneNo: String = "00000 000001"
  val communicationEmail: String = "communicationTestUser@test.com"

  val expectedCompanyDetails: DesignatoryDetails = DesignatoryDetails(
    DesignatoryDetailsName(Some(firstName), Some(surname)),
    Some(DesignatoryDetailsAddress(
      Some(companyAddressLine1),
      Some(companyAddressLine2),
      Some(companyAddressLine3),
      Some(companyAddressLine4),
      Some(companyAddressLine5),
      Some(companyPostcode),
      Some(companyForeignCountry))
    ),
    Some(DesignatoryDetailsContact(
      Some(DesignatoryDetailsTelephone(telephoneNumber = Some(companyPhoneNo))),
      Some(DesignatoryDetailsEmail(primary = Some(companyEmail))))
    )
  )

  val expectedCommunicationDetails: DesignatoryDetails = DesignatoryDetails(
    DesignatoryDetailsName(Some(firstName), Some(surname)),
    Some(DesignatoryDetailsAddress(
      Some(communicationAddressLine1),
      Some(communicationAddressLine2),
      Some(communicationAddressLine3),
      Some(communicationAddressLine4),
      Some(communicationAddressLine5),
      Some(communicationPostcode),
      Some(communicationForeignCountry))
    ),
    Some(DesignatoryDetailsContact(
      Some(DesignatoryDetailsTelephone(daytime = Some(communicationPhoneNo))),
      Some(DesignatoryDetailsEmail(primary = Some(communicationEmail))))
    )
  )

  val expectedCompanyCtDesignatoryDetails: CtDesignatoryDetailsCollection = CtDesignatoryDetailsCollection(
    Some(expectedCompanyDetails),
    Some(expectedCommunicationDetails)
  )

  "CtConnector" when {

    "Requesting a Ct account summary" should {

      "return an instance of CtAccountSummaryData for a valid response" in {

        val expectedAccountBalance: CtAccountBalance = CtAccountBalance(10.00)
        val expectedCtAccountSummary: CtAccountSummaryData = CtAccountSummaryData(
          accountBalance = expectedAccountBalance,
          effectiveDueDate = LocalDate.parse("2023-09-01")

        )

        StubConnector.withResponse(accountSummaryUrl)(OK, Some(testCtAccountSummaryData))

        val result: Future[Option[CtAccountSummaryData]] = connector.accountSummary(CtUtr(testCtUtr))

        await(result) mustBe Some(expectedCtAccountSummary)

        StubConnector.verifyRequest(accountSummaryUrl, count = 1)
      }

      "handle the account summary not being found" in {

        StubConnector.withResponse(accountSummaryUrl)(NOT_FOUND, None)

        val result: Future[Option[CtAccountSummaryData]] = connector.accountSummary(CtUtr(testCtUtr))

        await(result) mustBe None

        StubConnector.verifyRequest(accountSummaryUrl, count = 1)
      }

      "handle there being no content for the account summary" in {

        StubConnector.withResponse(accountSummaryUrl)(NO_CONTENT, None)

        val result: Future[Option[CtAccountSummaryData]] = connector.accountSummary(CtUtr(testCtUtr))

        await(result) mustBe None

        StubConnector.verifyRequest(accountSummaryUrl, count = 1)
      }

      "handle an internal server error" in {

        StubConnector.withResponse(accountSummaryUrl)(INTERNAL_SERVER_ERROR, None)

        val result: Future[Option[CtAccountSummaryData]] = connector.accountSummary(CtUtr(testCtUtr))

        await(result.failed) mustBe a[MicroServiceException]

        StubConnector.verifyRequest(accountSummaryUrl, count = 1)
      }

    }

    "Requesting a Ct account's designatory details" should {

      "return an instance of CTDesignatoryDetailsCollection for a valid response" in {

        StubConnector.withResponse(designatoryDetailsUrl)(OK, Some(testCtDesignatoryDetailsCollection))

        val result: Future[Option[CtDesignatoryDetailsCollection]] = connector.designatoryDetails(CtUtr(testCtUtr))

        await(result) mustBe Some(expectedCompanyCtDesignatoryDetails)

        StubConnector.verifyRequest(designatoryDetailsUrl, count = 1)
      }

      "handle the Ct accounts designatory details not being found" in {

        StubConnector.withResponse(designatoryDetailsUrl)(NOT_FOUND, None)

        val result: Future[Option[CtDesignatoryDetailsCollection]] = connector.designatoryDetails(CtUtr(testCtUtr))

        await(result) mustBe None

        StubConnector.verifyRequest(designatoryDetailsUrl, count = 1)
      }

      "handle there being no content for the Ct account's designatory details" in {

        StubConnector.withResponse(designatoryDetailsUrl)(NO_CONTENT, None)

        val result: Future[Option[CtDesignatoryDetailsCollection]] = connector.designatoryDetails(CtUtr(testCtUtr))

        await(result) mustBe None

        StubConnector.verifyRequest(designatoryDetailsUrl, count = 1)
      }

      "handle an internal server error" in {

        StubConnector.withResponse(designatoryDetailsUrl)(INTERNAL_SERVER_ERROR, None)

        val result: Future[Option[CtDesignatoryDetailsCollection]] = connector.designatoryDetails(CtUtr(testCtUtr))

        await(result.failed) mustBe a[MicroServiceException]

        StubConnector.verifyRequest(designatoryDetailsUrl, count = 1)
      }
    }
  }

}
