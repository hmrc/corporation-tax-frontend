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

package services

import base.SpecBase
import connectors.EnrolmentStoreConnector
import models.requests.AuthenticatedRequest
import models.{CtEnrolment, CtUtr, UserEnrolmentStatus, UserEnrolments}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateUtil

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with BeforeAndAfter with DateUtil {
  val activeOct13: UserEnrolmentStatus = UserEnrolmentStatus("IR-CT", Some("active"), Some(LocalDateTime.parse("2018-10-13T23:59:59.999")))
  val activeJan01: UserEnrolmentStatus = UserEnrolmentStatus("IR-CT", Some("active"), Some(LocalDateTime.parse("2018-01-01T23:59:59.999")))
  val activeFeb28: UserEnrolmentStatus = UserEnrolmentStatus("IR-CT", Some("active"), Some(LocalDateTime.parse("2018-02-28T23:59:59.999")))
  val noDate: UserEnrolmentStatus = UserEnrolmentStatus("IR-CT", Some("active"), None)

  val mockConnector: EnrolmentStoreConnector = mock[EnrolmentStoreConnector]
  val service = new EnrolmentStoreService(mockConnector)

  private val moreThan23DaysFromTokenExpiry = LocalDateTime.parse("2018-09-15T08:00:00.000").utcOffset

  private val lessThan23DaysFromTokenExpiry = LocalDateTime.parse("2018-09-25T08:00:00.000").utcOffset

  private val exactly23DaysFromTokenExpiry = LocalDateTime.parse("2018-09-20T23:59:59.999").utcOffset

  private val multipleRecords = LocalDateTime.parse("2018-02-01T17:36:00.000").utcOffset

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = Request(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )

  "EnrolmentStoreService" when {

    "showActivationLink is called" should {

      "return false when enrolment was requested within last 7 days" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
            .thenReturn(Future.successful(Right(UserEnrolments(List(activeOct13)))))

        service.showNewPinLink(
          enrolment = CtEnrolment(ctUtr = CtUtr("credId"), isActivated = false),
          currentDate = moreThan23DaysFromTokenExpiry).futureValue mustBe false
      }

      "return false when enrolment was requested within last 7 days and tax is activated" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(List(activeOct13)))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = true),
          lessThan23DaysFromTokenExpiry).futureValue mustBe false
      }

      "return false when multiple records are returned and latest enrolment record is within 7 days of current date" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(List(activeJan01, activeFeb28)))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          multipleRecords).futureValue mustBe false
      }

      "return false when enrolment was requested 7 days ago" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(List(activeOct13)))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          exactly23DaysFromTokenExpiry).futureValue mustBe false
      }

      "return true when enrolment was requested more than 7 days ago" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(List(activeOct13)))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          lessThan23DaysFromTokenExpiry).futureValue mustBe true
      }

      "return true if no enrolments were found" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(Nil))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          exactly23DaysFromTokenExpiry).futureValue mustBe true
      }

      "return true if enrolments were not able to be retrieved" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Left("Simulated Failure")))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          exactly23DaysFromTokenExpiry).futureValue mustBe true

      }

      "return true if for single enrolment with no enrolmentTokenExpiryDate set" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(List(noDate)))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          exactly23DaysFromTokenExpiry).futureValue mustBe true

      }

      "return true if for multiple enrolments with no enrolmentTokenExpiryDate set" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(List(noDate, noDate)))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          exactly23DaysFromTokenExpiry).futureValue mustBe true
      }

      "return false if for multiple enrolments with no enrolmentTokenExpiryDate set" in {
        when(mockConnector.getEnrolments(any())(any(), any()))
          .thenReturn(Future.successful(Right(UserEnrolments(List(noDate, noDate)))))

        service.showNewPinLink(CtEnrolment(CtUtr("credId"), isActivated = false),
          exactly23DaysFromTokenExpiry).futureValue mustBe true
      }
    }
  }
}
