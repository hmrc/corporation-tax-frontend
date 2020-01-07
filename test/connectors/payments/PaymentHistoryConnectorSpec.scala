/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors.payments

import base.SpecBase
import connectors.MockHttpClient
import models.payments.PaymentStatus.{Successful, Invalid}
import models.payments._
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.http.Status._

class PaymentHistoryConnectorSpec extends SpecBase with MockitoSugar with ScalaFutures with MockHttpClient {

  def testConnector[A](mockedResponse: HttpResponse, httpWrapper: HttpWrapper = mock[HttpWrapper]): PaymentHistoryConnector = {
    when(httpWrapper.getF[A](Matchers.any())).thenReturn(mockedResponse)
    val httpClient: HttpClient = http(httpWrapper)
    new PaymentHistoryConnector(httpClient, frontendAppConfig)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val ctUtr = CtUtr("utr")

  "The Payment History connector" when {

    "GET is called" should {
      "handle a valid 200 response with minimum data" in {
        val hodData = Json.parse(
          """
            |{
            |"searchScope": "bta",
            |"searchTag":"search-tag",
            |"payments": []
            |}
          """.stripMargin
        )
        val hodResponse = HttpResponse(OK, Some(hodData))
        val connector = testConnector(hodResponse)
        val result = connector.get("")
        whenReady(result) {
          _ mustBe Right(Nil)
        }

      }

      "handle a valid 200 response with single payments record" in {
        val hodResponse = HttpResponse(OK,
          Some(
            Json.parse(
              """
                |{
                |"searchScope": "bta",
                |"searchTag":"search-tag",
                |"payments": [
                | {
                |"reference" : "reference number",
                |"amountInPence" : 100,
                |"status": "Successful",
                |"createdOn": "data string",
                |"taxType" : "tax type"
                | }
                |]
                |}
              """.stripMargin
            )
          )
        )
        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(
            List(CtPaymentRecord("reference number", 100, Successful, "data string", "tax type"))
          )
        }
      }

      "handle a valid 200 response with multiple payment records" in {
        val hodResponse = HttpResponse(
          OK, Some(
            Json.parse(
              """
                |{
                |"searchScope": "bta",
                |"searchTag":"search-tag",
                |"payments": [
                | {
                |"reference" : "reference number",
                |"amountInPence" : 100,
                |"status": "Successful",
                |"createdOn": "data string",
                |"taxType" : "tax type"
                | },
                | {
                |"reference" : "reference number 2",
                |"amountInPence" : 2000000000,
                |"status": "Successful",
                |"createdOn": "data string",
                |"taxType" : "tax type"
                | }
                |]
                |}
              """.stripMargin
            )
          )
        )

        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(List(
            CtPaymentRecord("reference number", 100, Successful, "data string", "tax type"),
            CtPaymentRecord("reference number 2", 2000000000, Successful, "data string", "tax type")
          ))
        }
      }

      "handle an invalid status response within payment records" in {
        val hodResponse = HttpResponse(
          OK, Some(
            Json.parse(
              """
                |{
                |"searchScope": "bta",
                |"searchTag":"search-tag",
                |"payments": [
                | {
                |"reference" : "reference number",
                |"amountInPence" : 100,
                |"status": "not-supported",
                |"createdOn": "data string",
                |"taxType" : "tax type"
                | },
                | {
                |"reference" : "reference number 2",
                |"amountInPence" : 2000000000,
                |"status": "Successful",
                |"createdOn": "data string",
                |"taxType" : "tax type"
                | }
                |]
                |}
              """.stripMargin
            )
          )
        )
        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(List(
            CtPaymentRecord("reference number", 100, Invalid, "data string", "tax type"),
            CtPaymentRecord("reference number 2", 2000000000, Successful, "data string", "tax type")
          ))
        }
      }

      "handle an incomplete json object" in {
        val hodResponse = HttpResponse(
          OK, Some(
            Json.parse(
              """{"searchScope": "bta"}"""
            )
          )
        )
        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("unable to parse data from payment api")
        }
      }

      "handle an invalid json object" in {
        val hodResponse = HttpResponse(
          OK, Some(
            Json.toJson(
              """{"searchScope", }"""
            )
          )
        )

        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("unable to parse data from payment api")
        }
      }

      "handle 400 response" in {
        val hodResponse = HttpResponse(BAD_REQUEST, Some(Json.obj()))

        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("invalid request sent")
        }
      }

      "handle 404 response" in {
        val hodResponse = HttpResponse(NOT_FOUND, Some(Json.obj()))

        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(Nil)
        }
      }

      "handle invalid response code" in {
        val hodResponse = HttpResponse(CREATED, Some(Json.obj()))

        val connector = testConnector(hodResponse)
        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("couldn't handle response from payment api")
        }
      }

      "handle 5xx response" in {
        val hodResponse = HttpResponse(INTERNAL_SERVER_ERROR, Some(Json.obj()))

        val connector = testConnector(hodResponse)

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("exception thrown from payment api")
        }
      }
    }
  }

}
