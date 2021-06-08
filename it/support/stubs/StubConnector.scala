package support.stubs

import connectors.payments.SpjRequestBtaCt

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.http.Fault

import play.api.libs.json.Json

object StubConnector {

  def withResponse(url:String)(status: Int, optBody: Option[String]): Unit = {

    stubFor(get(urlEqualTo(url)) willReturn {
      val coreResponse: ResponseDefinitionBuilder = aResponse().withStatus(status)

      withOptionalBody(optBody, coreResponse)
    })

  }

  def withFailedResponse(url: String): Unit =

    stubFor(get(urlEqualTo(url)) willReturn {
      aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)
    })

  def withHtmlPartialResponse(url: String)(status: Int, optBody: Option[String]): Unit = {

    stubFor(get(urlEqualTo(url)) willReturn {

      val coreResponse: ResponseDefinitionBuilder = aResponse().withStatus(status)

      val headerResponse: ResponseDefinitionBuilder = coreResponse.withHeader("Content-Type", "text/html;charset=UTF-8")

      withOptionalBody(optBody, headerResponse)
    })
  }

  def withResponseForCtPayLinkPost(url: String, spjRequestBtaCt: SpjRequestBtaCt)(status: Int, optBody: Option[String]): Unit = {

    stubFor(post(urlEqualTo(url))
      .withRequestBody(equalToJson(Json.toJson(spjRequestBtaCt).toString)) willReturn {

      val coreResponse: ResponseDefinitionBuilder = aResponse().withStatus(status)

      withOptionalBody(optBody, coreResponse)
    })

  }

  def withFailedResponseForCtPayLinkPost(url: String, spjRequestBtaCt: SpjRequestBtaCt): Unit = {

    stubFor(post(urlEqualTo(url))
      .withRequestBody(equalToJson(Json.toJson(spjRequestBtaCt).toString)) willReturn {
      aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)
    })

  }



  def verifyRequest(url: String, count: Int): Unit = verify(count, getRequestedFor(urlEqualTo(url)))

  def verifyPostRequest(url: String, count: Int): Unit = verify(count, postRequestedFor(urlEqualTo(url)))

  private def withOptionalBody(body: Option[String], responseDefinitionBuilder: ResponseDefinitionBuilder): ResponseDefinitionBuilder =

    body match {
      case Some(body) => responseDefinitionBuilder.withBody(body)
      case _ => responseDefinitionBuilder
    }

}
