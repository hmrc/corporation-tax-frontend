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

package controllers.actions

import config.FrontendAppConfig
import controllers.routes
import javax.inject.Inject
import models.CtEnrolment
import models.requests.AuthenticatedRequest
import play.api.libs.json.Reads
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.AlternatePredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{OptionalRetrieval, Retrieval, ~}
import models.CtUtr
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector, config: FrontendAppConfig, defaultParser: PlayBodyParsers)
                              (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  val parser: BodyParser[AnyContent] = defaultParser.defaultBodyParser

  val ctUtr: Retrieval[Option[String]] = OptionalRetrieval("ctUtr", Reads.StringReads)

  private[controllers] def getEnrolment(enrolments: Enrolments): CtEnrolment = {
    enrolments.getEnrolment("IR-CT")
      .map(e => CtEnrolment(CtUtr(e.getIdentifier("UTR").map(_.value).getOrElse(throw new UnauthorizedException("Unable to retrieve ct utr"))), e.isActivated))
      .getOrElse(throw new UnauthorizedException("unable to retrieve ct enrolment"))
  }

  val activatedEnrolment: Enrolment = Enrolment("IR-CT")
  val notYetActivatedEnrolment: Enrolment = Enrolment("IR-CT", Seq(), "NotYetActivated")

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(AlternatePredicate(activatedEnrolment, notYetActivatedEnrolment)).retrieve(Retrievals.externalId and Retrievals.authorisedEnrolments) {
      case externalId ~ enrolments =>
        externalId.map {
          externalId => block(AuthenticatedRequest(request, externalId, getEnrolment(enrolments)))
        }.getOrElse(throw new UnauthorizedException("Unable to retrieve external Id"))
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: InsufficientEnrolments =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: InsufficientConfidenceLevel =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: UnsupportedAuthProvider =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: UnsupportedAffinityGroup =>
        Redirect(routes.UnauthorisedController.onPageLoad())
      case _: UnsupportedCredentialRole =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]