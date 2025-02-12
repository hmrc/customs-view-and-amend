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

import izumi.reflect.Tag
import org.scalamock.handlers.{CallHandler2, CallHandler4}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue
import play.api.libs.ws.{BodyWritable, WSRequest}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

trait HttpV2Support { this: MockFactory & Matchers =>

  implicit val hc: HeaderCarrier                            = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val mockHttp: HttpClientV2             = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  def mockHttpGet[A](
    url: URL
  )(response: A) = {
    mockGet[A](url)
    mockRequestBuilderExecuteWithoutException[A](response)
  }

  def mockHttpGetFailure(
    url: URL
  )(exception: Exception) = {
    mockGet(url)
    mockRequestBuilderExecuteWithException(exception)
  }

  def mockHttpPost[A](url: String, requestBody: JsValue, hasHeaders: Boolean = false)(response: A) = {
    mockPost(URL(url))
    mockRequestBuilderWithBody(requestBody)
    if hasHeaders then mockRequestBuilderTransform()
    mockRequestBuilderExecuteWithoutException(response)
  }
  def mockHttpPostWithException(
    url: String,
    requestBody: JsValue,
    hasHeaders: Boolean = false
  )(exception: Exception) = {
    mockPost(URL(url))
    mockRequestBuilderWithBody(requestBody)
    if hasHeaders then mockRequestBuilderTransform()
    mockRequestBuilderExecuteWithException(exception)
  }

  def mockHttpPostEmpty[A](url: String)(response: A) = {
    mockPost(URL(url))
    mockRequestBuilderExecuteWithoutException(response)
  }

  def mockHttpPostEmptyWithException[A](url: String)(exception: Exception) = {
    mockPost(URL(url))
    mockRequestBuilderExecuteWithException(exception)
  }

  private def mockGet[A](url: URL) =
    (mockHttp
      .get(_: URL)(_: HeaderCarrier))
      .expects(url, *)
      .returning(mockRequestBuilder)

  private def mockPost[A](url: URL) =
    (mockHttp
      .post(_: URL)(_: HeaderCarrier))
      .expects(url, *)
      .returning(mockRequestBuilder)

  private def mockRequestBuilderWithBody[JsValue](
    body: JsValue
  ): CallHandler4[JsValue, BodyWritable[JsValue], Tag[JsValue], ExecutionContext, RequestBuilder] =
    (mockRequestBuilder
      .withBody(_: JsValue)(_: BodyWritable[JsValue], _: Tag[JsValue], _: ExecutionContext))
      .expects(body, *, *, *)
      .returning(mockRequestBuilder)

  private def mockRequestBuilderExecuteWithoutException[A](
    value: A
  ): CallHandler2[HttpReads[A], ExecutionContext, Future[A]] =
    (mockRequestBuilder
      .execute(_: HttpReads[A], _: ExecutionContext))
      .expects(*, *)
      .returning(Future successful value)

  private def mockRequestBuilderTransform() =
    (mockRequestBuilder
      .transform(_: WSRequest => WSRequest))
      .expects(*)
      .returning(mockRequestBuilder)

  private def mockRequestBuilderExecuteWithException[A](
    ex: Exception
  ) =
    (mockRequestBuilder
      .execute(_: HttpReads[A], _: ExecutionContext))
      .expects(*, *)
      .returning(Future failed ex)

}
