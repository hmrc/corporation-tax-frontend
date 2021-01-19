/*
 * Copyright 2021 HM Revenue & Customs
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

package base


import controllers.actions.{AuthAction, ServiceInfoAction}
import models.CtEnrolment
import models.requests.{AuthenticatedRequest, ServiceInfoRequest}
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers, Request, Result}
import play.twirl.api.HtmlFormat
import models.CtUtr

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class FakeAuthAction(bodyParsers: PlayBodyParsers)(implicit val executionContext: ExecutionContext) extends AuthAction {
  val parser: BodyParser[AnyContent] = bodyParsers.default
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    block(AuthenticatedRequest(request, "id", CtEnrolment(CtUtr("utr"), isActivated = true)))
}

object FakeServiceInfoAction extends ServiceInfoAction {
  val executionContext: ExecutionContext = global

  override protected def transform[A](request: AuthenticatedRequest[A]): Future[ServiceInfoRequest[A]] = {
    Future.successful(ServiceInfoRequest(request, HtmlFormat.empty))
  }
}
