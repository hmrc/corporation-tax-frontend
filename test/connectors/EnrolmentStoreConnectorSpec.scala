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

package connectors

import _root_.models.{CtEnrolment, CtUtr, UserEnrolmentStatus, UserEnrolments}
import base.SpecBase
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
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnectorSpec  extends SpecBase with MockitoSugar with ScalaFutures {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  lazy val httpGet: HttpClientV2 = mock[HttpClientV2]
  lazy val requestBuilder: RequestBuilder = mock[RequestBuilder]

  when(httpGet.get(any())(any())).thenReturn(requestBuilder)

  val connector = new EnrolmentStoreConnector(httpGet,frontendAppConfig)
  def result: Future[Either[String, UserEnrolments]] = connector.getEnrolments("cred-id")

  "EnrolmentStoreConnectorSpec" when {
    "getEnrolments is called" should {
      "handle a 200 response with a single enrolment" in {
        val body = Json.parse(
          """
            |{
            |"enrolments":[
            |{
            |"service":"IR-PAYE",
            |"state":"active",
            |"enrolmentTokenExpiryDate":"2018-10-13 17:36:00.000"
            |}]
            |}
            """.stripMargin).toString()
        val httpResponse = HttpResponse(status = OK, body = body, headers = Map.empty)

        when(requestBuilder.execute[HttpResponse](any(), any())).thenReturn(
          Future.successful(httpResponse)
        )
        val expected = Right(
          UserEnrolments(
            List(
              UserEnrolmentStatus("IR-PAYE", Some("active"), Some(LocalDateTime.parse("2018-10-13T17:36:00.000")))
            )
          )
        )

        result.futureValue mustBe expected
      }
      "handle a 200 response with multiple enrolments" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(HttpResponse(status = OK, json = Json.parse(
            """
              |{
              |"enrolments":[
              |{
              |"service":"IR-PAYE",
              |"state":"active",
              |"enrolmentTokenExpiryDate":"2018-10-13 17:36:00.000"
              |},
              |{"service":"VAT",
              |"state":"active",
              |"enrolmentTokenExpiryDate":"2018-10-13 17:36:00.000"
              |},
              |{"service":"SA",
              |"state":"active",
              |"enrolmentTokenExpiryDate":"2018-10-13 17:36:00.000"
              |}
              |]
              |}
            """.stripMargin), headers = Map.empty))
        )
        result.futureValue mustBe Right(
          UserEnrolments(
            List(
              UserEnrolmentStatus(
                "IR-PAYE", Some("active"), Some(LocalDateTime.parse("2018-10-13T17:36:00.000"))
              ),
              UserEnrolmentStatus(
                "VAT", Some("active"), Some(LocalDateTime.parse("2018-10-13T17:36:00.000"))
              ),
              UserEnrolmentStatus(
                "SA", Some("active"), Some(LocalDateTime.parse("2018-10-13T17:36:00.000"))
              )
            )
          )
        )
      }
      "handle a 200 response with invalid JSON" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(
            HttpResponse.apply(
              OK,
              Json.parse(
              """
                |{
                |"enrolments":""
                |}
              """.stripMargin),
              Map.empty[String, Seq[String]]
            )
          )
        )
        result.futureValue mustBe Left("Unable to parse data from enrolment API")
      }
      "handle a 404 response" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(HttpResponse(NOT_FOUND, ""))
        )
        result.futureValue mustBe Left("User not found from enrolment API")
      }
      "handle a 400 response" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(HttpResponse(BAD_REQUEST, ""))
        )
        result.futureValue mustBe Left("Bad request to enrolment API")
      }
      "handle a 403 response" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(HttpResponse(FORBIDDEN, ""))
        )
        result.futureValue mustBe Left("Forbidden from enrolment API")
      }
      "handle a 503 response" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(HttpResponse(SERVICE_UNAVAILABLE, ""))
        )
        result.futureValue mustBe Left("Unexpected error from enrolment API")
      }
      "handle a 204 response" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(HttpResponse(NO_CONTENT, ""))
        )
        result.futureValue mustBe Left("No content from enrolment API")
      }
      "handle a failed response from server" in {
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        )
        result.futureValue mustBe Left("Exception thrown from enrolment API")
      }
      "handle an incorrect code response" in {
        val imaginaryCode = 823
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(
          Future.successful(HttpResponse(imaginaryCode, ""))
        )
        result.futureValue mustBe Left("Enrolment API couldn't handle response code")
      }
    }
  }
}
