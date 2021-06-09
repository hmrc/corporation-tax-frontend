package support

object TestJsonObjects {

  val testCtAccountSummaryData: String =
  """|{
     | "accountBalance":
     |   {
     |     "amount": 10.00
     |   }
     |}""".stripMargin

  val testCtDesignatoryDetailsCollection: String =
  """|{
     | "company":
     | {
     |   "name":
     |   {
     |     "nameLine1": "testUserFirstName",
     |     "nameLine2": "testUserSurname"
     |   },
     |   "address":
     |   {
     |     "addressLine1": "Test company address line 1",
     |     "addressLine2": "Test company address line 2",
     |     "addressLine3": "Test company address line 3",
     |     "addressLine4": "Test company address line 4",
     |     "addressLine5": "Test company address line 5",
     |     "postcode": "XX10 1XX",
     |     "foreignCountry": "N"
     |   },
     |   "contact":
     |   {
     |     "telephone":
     |      {
     |        "telephoneNumber":"00000 000000"
     |      },
     |     "email":
     |      {
     |        "primary": "companyTestUser@test.com"
     |      }
     |    }
     | },
     | "communication":
     | {
     |   "name":
     |   {
     |     "nameLine1": "testUserFirstName",
     |     "nameLine2": "testUserSurname"
     |   },
     |   "address":
     |   {
     |     "addressLine1": "Test communication address line 1",
     |     "addressLine2": "Test communication address line 2",
     |     "addressLine3": "Test communication address line 3",
     |     "addressLine4": "Test communication address line 4",
     |     "addressLine5": "Test communication address line 5",
     |     "postcode": "YY10 1YY",
     |     "foreignCountry": "Y"
     |   },
     |   "contact":
     |   {
     |     "telephone":
     |     {
     |       "daytime": "00000 000001"
     |     },
     |     "email":
     |     {
     |       "primary": "communicationTestUser@test.com"
     |     }
     |   }
     | }
     |}""".stripMargin

  val testSingleUserEnrolment: String =
  """ |{
      |  "enrolments":
      |  [
      |    {
      |      "service": "test-service-1",
      |      "state": "active",
      |      "enrolmentTokenExpiryDate": "2028-01-01 12:00:00.000"
      |    }
      |  ]
      |}""".stripMargin

  val testMultipleUserEnrolments: String =
  """ |{
      |"enrolments":
      |  [
      |    {
      |      "service": "test-service-1",
      |      "state": "active",
      |      "enrolmentTokenExpiryDate": "2028-01-01 12:00:00.000"
      |    },
      |    {
      |      "service": "test-service-2",
      |      "state": "inactive",
      |      "enrolmentTokenExpiryDate": "2028-01-02 23:59:59.999"
      |    },
      |    {
      |      "service": "test-service-3",
      |      "state": "active",
      |      "enrolmentTokenExpiryDate": "2028-01-03 00:00:00.000"
      |     }
      |  ]
      |}""".stripMargin

  val testInvalidUserEnrolments: String =
  """|{
     |  "enrolments":
     |  [
     |    {
     |      "state": "active",
     |      "enrolmentTokenExpiryDate": "2020-01-01 00:00:00.000"
     |    }
     |  ]
     |}""".stripMargin

  val testNextUrlSuccess: String =
  """|{
     |  "nextUrl": "https://www.tax.service.gov.uk/pay/12345/choose-a-way-to-pay"
     |}""".stripMargin

  val testEmptyCtPaymentList: String =
  """|{
     |  "searchScope": "bta",
     |  "searchTag": "search-tag",
     |  "payments": []
     |}""".stripMargin

  val testSingleEntryCtPaymentList: String =
  """|{
     |  "searchScope": "bta",
     |  "searchTag": "search-tag",
     |  "payments":
     |  [
     |    {
     |      "reference": "Test reference 001",
     |      "amountInPence": 10,
     |      "status": "Successful",
     |      "createdOn": "Some date 001",
     |      "taxType": "Tax type 001"
     |    }
     |  ]
     |}""".stripMargin

  val testMultipleEntryCtPaymentList: String =
    """|{
       |  "searchScope": "bta",
       |  "searchTag": "search-tag",
       |  "payments":
       |  [
       |    {
       |      "reference": "Test reference 001",
       |      "amountInPence": 10,
       |      "status": "Successful",
       |      "createdOn": "Some date 001",
       |      "taxType": "Tax type 001"
       |    },
       |    {
       |      "reference": "Test reference 002",
       |      "amountInPence": -10,
       |      "status": "Invalid",
       |      "createdOn": "Some date 002",
       |      "taxType": "Tax type 002"
       |    },
       |    {
       |      "reference": "Test reference 003",
       |      "amountInPence": 2001,
       |      "status": "Successful",
       |      "createdOn": "Some date 003",
       |      "taxType": "Tax type 003"
       |    }
       |  ]
       |}""".stripMargin

  val testIncompleteCtPayment: String =
    """|{
       |  "searchScope": "bta",
       |  "searchTag": "search-tag",
       |  "payments":
       |  [
       |    {
       |      "reference": "Test reference 001",
       |      "status": "Successful",
       |      "createdOn": "Some date 001",
       |      "taxType": "Tax type 001"
       |    }
       |  ]
       |}""".stripMargin
}
