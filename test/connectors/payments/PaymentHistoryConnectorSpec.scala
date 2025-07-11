/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{CtEnrolment, CtUtr}
import models.payments.PaymentStatus.{Invalid, Successful}
import models.payments._
import models.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PaymentHistoryConnectorSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val mockHttp: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]

  when(mockHttp.get(any())(any())).thenReturn(requestBuilder)

  val connector = new PaymentHistoryConnector(mockHttp, frontendAppConfig)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  val ctUtr: CtUtr = CtUtr("utr")

  "The Payment History connector" when {

    "GET is called" should {
      "handle a valid 200 response with minimum data" in {
        val hodData = Json.parse(
          """
            |{
            |  "searchScope": "bta",
            |  "searchTag":"search-tag",
            |  "payments": []
            |}
          """.stripMargin
        )

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, hodData, Map.empty[String, Seq[String]])))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(Nil)
        }

      }

      "handle a valid 200 response with single payments record" in {
        val hodResponse = HttpResponse(
          OK,
          Json.parse(
            """
              |{
              |  "searchScope": "bta",
              |  "searchTag":"search-tag",
              |  "payments": [
              |    {
              |      "reference" : "reference number",
              |      "amountInPence" : 3,
              |      "status": "Successful",
              |      "createdOn": "data string",
              |      "taxType" : "tax type"
              |    }
              |  ]
              |}
            """.stripMargin),
            Map.empty[String, Seq[String]]
        )

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(
            List(CtPaymentRecord("reference number", 3, Successful, "data string", "tax type"))
          )
        }
      }

      "handle a valid 200 response with multiple payment records" in {
        val hodResponse = HttpResponse(
          OK,
          Json.parse(
            """
              |{
              | "searchScope": "bta",
              | "searchTag":"search-tag",
              | "payments": [
              |   {
              |     "reference" : "reference number",
              |     "amountInPence" : 3,
              |     "status": "Successful",
              |     "createdOn": "data string",
              |     "taxType" : "tax type"
              |   },
              |   {
              |     "reference" : "reference number 2",
              |     "amountInPence" : 2,
              |     "status": "Successful",
              |     "createdOn": "data string",
              |     "taxType" : "tax type"
              |   }
              | ]
              |}
            """.stripMargin
          ),
          Map.empty[String, Seq[String]]
        )

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(List(
            CtPaymentRecord("reference number", 3, Successful, "data string", "tax type"),
            CtPaymentRecord("reference number 2", 2, Successful, "data string", "tax type")
          ))
        }
      }

      "handle an invalid status response within payment records" in {
        val hodResponse = HttpResponse(
          OK,
          Json.parse(
            """
              |{
              |  "searchScope": "bta",
              |  "searchTag":"search-tag",
              |  "payments": [
              |    {
              |      "reference" : "reference number",
              |      "amountInPence" : 1,
              |      "status": "not-supported",
              |      "createdOn": "data string",
              |      "taxType" : "tax type"
              |    },
              |    {
              |      "reference" : "reference number 2",
              |      "amountInPence" : 2,
              |      "status": "Successful",
              |      "createdOn": "data string",
              |      "taxType" : "tax type"
              |    }
              |  ]
              |}
            """.stripMargin
          ),
          Map.empty[String, Seq[String]]
        )

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(List(
            CtPaymentRecord("reference number", 1, Invalid, "data string", "tax type"),
            CtPaymentRecord("reference number 2", 2, Successful, "data string", "tax type")
          ))
        }
      }

      "handle an incomplete json object" in {
        val hodResponse = HttpResponse(
          OK,
          Json.parse(
            """{"searchScope": "bta"}"""
          ),
          Map.empty[String, Seq[String]]
        )

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("unable to parse data from payment api")
        }
      }

      "handle an invalid json object" in {
        val hodResponse = HttpResponse(
          OK,
          Json.toJson(
            """{"searchScope", }"""
          ),
          Map.empty[String, Seq[String]]
        )


        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("unable to parse data from payment api")
        }
      }

      "handle 400 response" in {
        val hodResponse = new BadRequestException("oops")

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("invalid request sent")
        }
      }

      "handle 404 response" in {
        val hodResponse = new NotFoundException("oops")

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Right(Nil)
        }
      }

      "handle other 4xx response" in {
        val hodResponse = UpstreamErrorResponse("403", FORBIDDEN, FORBIDDEN)

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("exception thrown from payment api")
        }
      }

      "handle invalid response code" in {
        val hodResponse = HttpResponse(CREATED, Json.obj(), Map.empty[String, Seq[String]])

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("couldn't handle response from payment api")
        }
      }

      "handle 5xx response" in {
        val hodResponse = UpstreamErrorResponse("500", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

        when(requestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(hodResponse))

        val result = connector.get("")

        whenReady(result) {
          _ mustBe Left("exception thrown from payment api")
        }
      }
    }
  }

}
