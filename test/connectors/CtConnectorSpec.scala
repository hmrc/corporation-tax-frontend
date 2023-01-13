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

import base.SpecBase
import models._
import models.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Upstream5xxResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CtConnectorSpec extends SpecBase with MockitoSugar with ScalaFutures {
  
  val mockHttp: HttpClient = mock[HttpClient]

  val connector = new CtConnector(mockHttp, frontendAppConfig)

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val ctUtr: CtUtr = CtUtr("utr")

  "CtConnector account summary" should {

    "call the micro service with the correct uri and return the contents" in {

      val ctAccountSummary = CtAccountSummaryData(Some(CtAccountBalance(Some(4.0))))

      when(mockHttp.GET[Option[CtAccountSummaryData]](any(), any(), any())(any(),any(),any()))
        .thenReturn(Future.successful(Some(ctAccountSummary)))

      val response = connector.accountSummary(ctUtr)

      whenReady(response) { r =>
        r mustBe Some(ctAccountSummary)
      }

    }

    "call the micro service with the correct uri and return no contents if there are none" in {
      when(mockHttp.GET[Option[CtAccountSummaryData]](any(), any(), any())(any(),any(),any()))
        .thenReturn(Future.successful(None))

      val response = connector.accountSummary(ctUtr)

      whenReady(response) { r =>
        r mustBe None
      }
    }

    "call the micro service and return 500" in {
      when(mockHttp.GET[Option[CtAccountSummaryData]](any(), any(), any())(any(),any(),any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("500", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val response = connector.accountSummary(ctUtr)

      whenReady(response.failed) { mse =>
        mse mustBe an[UpstreamErrorResponse]
      }
    }
  }

  val sampleDesignatoryDetails: JsValue = Json.parse(
    """{
      |  "company": {
      |    "name": {
      |      "nameLine1": "A B",
      |      "nameLine2": "xyz"
      |    },
      |    "address": {
      |      "addressLine1": "1 Fake Street",
      |      "addressLine2": "X",
      |      "addressLine3": "Nowhere",
      |      "addressLine4": "Nowhere",
      |      "addressLine5": "Nowhere",
      |      "postcode": "AA00 0AA",
      |      "foreignCountry": "France"
      |    }
      |  },
      |  "communication": {
      |    "name": {
      |      "nameLine1": "A B",
      |      "nameLine2": "xyz"
      |    },
      |    "address": {
      |      "addressLine1": "1 Fake Street",
      |      "addressLine2": "X",
      |      "addressLine3": "Nowhere",
      |      "addressLine4": "Nowhere",
      |      "addressLine5": "Nowhere",
      |      "postcode": "AA00 0AA",
      |      "foreignCountry": "France"
      |    }
      |  }
      |}""".stripMargin)

  "CtConnector designatory details" should {
    "Return the correct response for an example with designatory details information" in {
      when(mockHttp.GET[Option[CtDesignatoryDetailsCollection]](any(), any(), any())(any(),any(),any()))
        .thenReturn(Future.successful(Some(sampleDesignatoryDetails.as[CtDesignatoryDetailsCollection])))

      val response = connector.designatoryDetails(ctUtr)

      val expectedDetails = Some(DesignatoryDetails(
        DesignatoryDetailsName(Some("A B"), Some("xyz")),
        Some(DesignatoryDetailsAddress(
          Some("1 Fake Street"),
          Some("X"),
          Some("Nowhere"),
          Some("Nowhere"),
          Some("Nowhere"),
          Some("AA00 0AA"),
          Some("France")
        ))
      ))

      val expected = CtDesignatoryDetailsCollection(expectedDetails, expectedDetails)

      whenReady(response) { r =>
        r mustBe Some(expected)
      }
    }

    "call the micro service and return 500" in {
      when(mockHttp.GET[Option[CtDesignatoryDetailsCollection]](any(), any(), any())(any(),any(),any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("500", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val response = connector.designatoryDetails(ctUtr)

      whenReady(response.failed) { mse =>
        mse mustBe an[UpstreamErrorResponse]
      }
    }
  }


}
