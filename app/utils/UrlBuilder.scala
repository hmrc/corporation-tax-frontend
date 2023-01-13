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

package utils


import play.api.mvc.Request

import scala.annotation.tailrec

trait UrlBuilder extends LoggingUtil {

  def buildUrl(destinationUrl: String, tags: Seq[(String, Option[Any])])
              (implicit request: Request[_]): String = {
    resolvePlaceHolder(destinationUrl, tags)
  }

  @tailrec
  private def resolvePlaceHolder(url: String, tags: Seq[(String, Option[Any])])
                                (implicit request: Request[_]): String =
    if (tags.isEmpty) url
    else resolvePlaceHolder(replace(url, tags.head), tags.tail)

  private def replace(url: String, tags: (String, Option[Any]))
                     (implicit request: Request[_]): String = {
    val (tagName, tagValueOption) = tags
    tagValueOption match {
      case Some(valueOfTag) => url.replace(tagName, valueOfTag.toString)
      case _ =>
        if (url.contains(tagName)) {
          errorLog(s"Failed to populate parameter $tagName in URL $url")
        }
        url
    }
  }
}

object UrlBuilder extends UrlBuilder

