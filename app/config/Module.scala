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

package config

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.hooks.HookData.{FromMap, FromString}
import uk.gov.hmrc.http.hooks.{Data, HttpHook, RequestData, ResponseData}
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.AnsiColor._
import play.api.libs.json.Json
import scala.util.Success
import scala.util.Failure

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector]).asEagerSingleton()
    bind(classOf[HttpClient]).to(classOf[DebuggingHttpClient])
    // binding additional interfaces so that libraries that depend on http-verbs can be easily injected
    bind(classOf[CoreGet]).to(classOf[DebuggingHttpClient])
  }
}

@Singleton
class DebuggingHttpClient @Inject() (
  config: Configuration,
  override val httpAuditing: HttpAuditing,
  override val wsClient: WSClient,
  override protected val actorSystem: ActorSystem
) extends DefaultHttpClient(config, httpAuditing, wsClient, actorSystem) {

  override val hooks: Seq[HttpHook] = Seq(httpAuditing.AuditingHook, new DebuggingHook(config))
}

class DebuggingHook(config: Configuration) extends HttpHook {

  val shouldDebug: Boolean =
    config.underlying.getBoolean("outboundRequests.debug")

  override def apply(
    verb: String,
    url: URL,
    request: RequestData,
    responseF: Future[ResponseData]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit = {
    if (shouldDebug && !url.getPath().contains("/auth/authorise")) {
      responseF.andThen {
        case Success(response)  =>
          Logger("OutboundRequest").debug(
            s"""$printRequest  
          |$YELLOW Response: $BOLD${response.status}$RESET
          |   ${response.headers.toSeq
                .flatMap { case (k, vs) => vs.map(v => s"$BLUE$k: $MAGENTA$v$RESET") }
                .mkString("\n   ")}
          |      
          |${response.headers.find(_._1.toLowerCase() == "content-type") match {
                case Some((key, values)) if values.exists(_.startsWith("application/json")) =>
                  s"$GREEN${Json.prettyPrint(Json.parse(response.body.value))}$RESET\n"

                case _ => response.body.value
              }}""".stripMargin
          )
        case Failure(exception) =>
          Logger("OutboundRequest").debug(
            s"""$printRequest
            |$RED Failure: $BOLD${exception.toString()}$RESET
            |""".stripMargin
          )
      }
    }

    def printRequest =
      s"""
        | $BOLD$YELLOW$verb $CYAN$url$RESET 
        |   ${request.headers.toSeq.map { case (k, v) => s"$BLUE$k: $MAGENTA$v$RESET" }.mkString("\n   ")}
        |   ${request.body
          .map { case Data(value, isTruncated, isRedacted) =>
            value match {
              case FromMap(m)    =>
                m.toSeq
                  .flatMap { case (k, vs) => vs.map(v => (k, v)) }
                  .map { case (k, v) => s"$k = $v" }
                  .mkString("\n   ")
              case FromString(s) =>
                request.headers.find(_._1.toLowerCase() == "content-type") match {
                  case Some(value) if value.toString().startsWith("application/json") =>
                    s"$GREEN${Json.prettyPrint(Json.parse(s))}$RESET"

                  case _ => s
                }
            }
          }
          .getOrElse("")}""".stripMargin
  }

}
