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

import uk.gov.hmrc.http.cache.client.CacheMap
import base.SpecBase
import controllers.actions.{AuthAction, DataRetrievalAction, FakeDataRetrievalAction}
import models.CtEnrolment
import models.requests.AuthenticatedRequest
import org.scalatest.BeforeAndAfterEach
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import org.mockito.{Answers, Matchers}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.domain.CtUtr

import scala.concurrent.Future

trait ControllerSpecBase extends SpecBase  with MockitoSugar {

  val mockAuthAction:AuthAction = mock[AuthAction]
  val mockDataRetrievalAction = mock[DataRetrievalAction]

  def setupFakeAuthAction  = when(mockAuthAction.invokeBlock(Matchers.any(), Matchers.any())).thenAnswer(
    new Answer[Future[Result]]{
      def answer(var1: InvocationOnMock):Future[Result] = {
        val request = var1.getArguments()(0).asInstanceOf[Request[_]]
        val block = var1.getArguments()(1).asInstanceOf[AuthenticatedRequest[_] => Future[Result]]
        block(AuthenticatedRequest(request, "id", CtEnrolment(CtUtr("utr"), isActivated = true)))
      }
    }
  )

  val cacheMapId = "id"

  def emptyCacheMap = CacheMap(cacheMapId, Map())

  def getEmptyCacheMap = new FakeDataRetrievalAction(Some(emptyCacheMap))

  def dontGetAnyData = new FakeDataRetrievalAction(None)
}
