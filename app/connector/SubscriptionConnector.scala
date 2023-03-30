package connector

import com.google.inject.ImplementedBy
import config.AppConfig
import models.Subscription
import models.responses.SubscriptionResponse
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[SubscriptionConnectorImpl])
trait SubscriptionConnector {
  def getSubscription(implicit hc: HeaderCarrier): Future[Option[Subscription]]
}

@Singleton
class SubscriptionConnectorImpl @Inject() (httpClient: HttpClient) (implicit
                                                                executionContext: ExecutionContext,
                                                                appConfig: AppConfig
) extends Logging {
  private val baseUrl = ???
  private val getSubscriptionUrl = s"$baseUrl/eori/xi`"


  final def getSubscription(implicit hc: HeaderCarrier): Future[Option[Subscription]] = httpClient
    .GET[SubscriptionResponse](getSubscriptionUrl)
    .map {
        response => {
          Some(
            Subscription(
              response.eoriGB,
              response.eoriXI
            )
          )
        }
      }
    .recover { case _ => None }
}
