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
import models.{CtEnrolment, CtUtr, SpjRequestBtaCt}
import models.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentConnectorSpec extends SpecBase with MockitoSugar with ScalaFutures {

  private val testAmount = 1000
  private val testDate = LocalDate.parse("2023-09-01").format(DateTimeFormatter.ISO_LOCAL_DATE)
  private val testBackReturnUrl = "https://www.tax.service.gov.uk/business-account"
  private val testSpjRequest = SpjRequestBtaCt(testAmount, testBackReturnUrl, testBackReturnUrl, "123456789", testDate)

  val mockHttp: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]

  when(mockHttp.post(any())(any())).thenReturn(requestBuilder)
  when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)

  val connector = new PaymentConnector(mockHttp, frontendAppConfig)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  "PaymentConnector" when {
    "ctPayLink is called" should {
      "return a NextUrl if the external service is responsive" in {
        val nextUrl = NextUrl("https://www.tax.service.gov.uk/pay/12345/choose-a-way-to-pay")

        when(requestBuilder.execute[NextUrl](any, any))
          .thenReturn(Future.successful(nextUrl))

        val response = connector.ctPayLink(testSpjRequest)

        whenReady(response) { r =>
          r mustBe nextUrl
        }
      }

      "return the service-unavailable page if there is a problem" in {
        val nextUrl = NextUrl("http://localhost:9050/pay-online/service-unavailable")

        when(requestBuilder.execute(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("oops")))

        val response = connector.ctPayLink(testSpjRequest)

        whenReady(response) { r =>
          r mustBe nextUrl
        }
      }
    }
  }
}