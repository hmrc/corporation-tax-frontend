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

import config.FrontendAppConfig
import models.requests.{AuthenticatedRequest, NavContent}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, StringContextOps}
import utils.LoggingUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ServiceInfoPartialConnector @Inject()(val http: HttpClientV2,
                                            val config: FrontendAppConfig) extends LoggingUtil with HttpReadsInstances {

  private lazy val btaNavLinksUrl: String = config.btaUrl + "/business-account/partial/nav-links"

  def getNavLinks()(implicit hc: HeaderCarrier, ec: ExecutionContext, request: AuthenticatedRequest[_]): Future[Option[NavContent]] = {
    infoLog(s"[ServiceInfoPartialConnector][getNavLinks] - Attempted with: $btaNavLinksUrl")
    http.get(url"$btaNavLinksUrl").execute[Option[NavContent]]
      .recover {
        case e =>
          warnLog(s"[ServiceInfoPartialConnector][getNavLinks] - Unexpected error ${e.getMessage}")
          None
      }
  }
}
