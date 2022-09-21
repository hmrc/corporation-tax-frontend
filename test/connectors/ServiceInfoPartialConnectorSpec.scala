/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.ServiceInfoController
import models.requests.{AuthenticatedRequest, ListLinks, NavContent, NavLinks, ServiceInfoRequest}
import models.{CtEnrolment, CtUtr}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{AnyContent, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import services.PartialService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.service_info

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceInfoPartialConnectorSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  implicit val request: ServiceInfoRequest[_] = ServiceInfoRequest(
    AuthenticatedRequest(FakeRequest(), "", CtEnrolment(CtUtr("utr"), isActivated = true)),
    HtmlFormat.empty
  )
  val mockPartialService: PartialService = mock[PartialService]
  val mockServiceInfoPartialConnector: ServiceInfoPartialConnector = mock[ServiceInfoPartialConnector]
  val testView: service_info = app.injector.instanceOf[service_info]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val ctEnrolment: CtEnrolment = CtEnrolment(CtUtr("utr"), isActivated = true)

  val testController = new ServiceInfoController(mockServiceInfoPartialConnector, testView , mockMcc, mockPartialService)


  "ServiceInfoController" should {
    "retrieve the correct Model and return HTML" in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

      val navContent = NavContent(
        NavLinks("testEnHome", "testCyHome", "testUrl"),
        NavLinks("testEnAccount", "testCyAccount", "testUrl"),
        NavLinks("testEnMessages", "testCyMessages", "testUrl"),
        NavLinks("testEnHelp", "testCyHelp", "testUrl"),
        NavLinks("testEnForm", "testCyForm", "testUrl", Some(1)),
      )

      val listLinks: Seq[ListLinks] = Seq(
        ListLinks("testEnHome", "testUrl"),
        ListLinks("testEnAccount", "testUrl"),
        ListLinks("testEnMessages", "testUrl", Some("0")),
        ListLinks("testEnForm", "testUrl", Some("1")),
        ListLinks("testEnHelp", "testUrl"),
      )

      when(mockServiceInfoPartialConnector.getNavLinks()(any(), any(), any()))
        .thenReturn(Future.successful(Some(navContent)))

      when(mockPartialService.partialList(any())(any())).thenReturn(listLinks)

      val result = testController.serviceInfoPartial(AuthenticatedRequest(FakeRequest(), "", ctEnrolment))

      whenReady(result) { response =>
        response.toString must include (testView.apply(listLinks).toString())
      }
    }

    "retrieve the empty Model and empty HTML" in {

      implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

      when(mockServiceInfoPartialConnector.getNavLinks()(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(mockPartialService.partialList(any())(any())).thenReturn(Seq())

      val result = testController.serviceInfoPartial(AuthenticatedRequest(FakeRequest(), "", ctEnrolment))


      whenReady(result) { response =>
        response.toString must include (testView.apply(Seq()).toString())
      }
    }
  }



}