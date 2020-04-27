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

package services

import base.SpecBase
import connectors.CtConnector
import connectors.models.{CtAccountBalance, CtAccountSummaryData}
import models._
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CtServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockCtConnector: CtConnector = mock[CtConnector]

  val service = new CtService(mockCtConnector)

  val ctEnrolment = CtEnrolment(CtUtr("utr"), isActivated = true)

  val ctAccountSummary = CtAccountSummaryData(Some(CtAccountBalance(Some(1200.0))))

  "The CtService fetchCtModel method" when {
    "the connector return data" should {
      "return Right(CtData)" in {
        reset(mockCtConnector)
        when(mockCtConnector.accountSummary(ctEnrolment.ctUtr)).thenReturn(Future.successful(Option(ctAccountSummary)))

        whenReady(service.fetchCtModel(ctEnrolment)) {
          _ mustBe Right(Some(CtData(ctAccountSummary)))
        }
      }
    }
    "the connector returns no data" should {
      "return Right(None)" in {
        reset(mockCtConnector)
        when(mockCtConnector.accountSummary(ctEnrolment.ctUtr)).thenReturn(Future.successful(None))

        whenReady(service.fetchCtModel(ctEnrolment)) {
          _ mustBe Right(None)
        }
      }
    }
    "the connector throws an exception" should {
      "return Left(CtGenericError)" in {
        reset(mockCtConnector)
        when(mockCtConnector.accountSummary(ctEnrolment.ctUtr)).thenReturn(Future.failed(new Throwable))

        whenReady(service.fetchCtModel(ctEnrolment)) {
          _ mustBe Left(CtGenericError)
        }
      }
    }
    "the ct enrolment is not activated" should {
      "return a Left(CtUnactivated)" in {
        reset(mockCtConnector)

        whenReady(service.fetchCtModel(CtEnrolment(CtUtr("utr"), isActivated = false))) {
          _ mustBe Left(CtUnactivated)
        }
      }
    }
  }

}
