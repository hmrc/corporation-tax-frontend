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

package controllers

import base.SpecBase
import config.{CorporationTaxHeaderCarrierForPartialsConverter, FrontendAppConfig}
import connectors.{DataCacheConnector, ServiceInfoPartialConnector}
import controllers.actions.{AuthAction, DataRetrievalAction, FakeDataRetrievalAction, ServiceInfoAction}
import models.CtEnrolment
import models.requests.AuthenticatedRequest
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

trait ControllerSpecBase extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  final val mockAuthAction: AuthAction = spy(
    new AuthAction(
      mock[AuthConnector],
      mock[FrontendAppConfig]
    )(
      ExecutionContext.fromExecutor(ExecutionContext.global)
    )
  )

  final val mockServiceInfoConnector = mock[ServiceInfoPartialConnector]

  final lazy val mockedServiceInfoAction = new ServiceInfoAction(
    mockServiceInfoConnector,
    injector.instanceOf[CorporationTaxHeaderCarrierForPartialsConverter]
  )(
    ExecutionContext.fromExecutor(ExecutionContext.global)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthAction)
    reset(mockServiceInfoConnector)
  }

  def setupFakeServiceAction(partial: Html) =
    when(mockServiceInfoConnector.getServiceInfoPartial()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(partial))

  def setupFakeAuthAction[A] = doAnswer(
    new Answer[Future[Result]] {
      def answer(var1: InvocationOnMock): Future[Result] = {
        val request = var1.getArguments()(0).asInstanceOf[Request[A]]
        val block = var1.getArguments()(1).asInstanceOf[AuthenticatedRequest[A] => Future[Result]]
        block(AuthenticatedRequest(request, "id", CtEnrolment(CtUtr("utr"), isActivated = true)))
      }
    }
  ).when(mockAuthAction).invokeBlock[A](Matchers.any(), Matchers.any())

  val cacheMapId = "id"

  def emptyCacheMap = CacheMap(cacheMapId, Map())
  //the tests at present do not need this to be exercised, it just exists to satisfy the interface
  val mockDataCacheConnector = mock[DataCacheConnector]

  def getEmptyCacheMap = new FakeDataRetrievalAction(mockDataCacheConnector)(Some(emptyCacheMap))

  def dontGetAnyData = new FakeDataRetrievalAction(mockDataCacheConnector)(None)
}
