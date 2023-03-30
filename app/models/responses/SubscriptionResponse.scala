package models.responses

import play.api.libs.json.{Json, OFormat}

case class SubscriptionResponse (
  eoriGB: String,
  eoriXI: String
)

object SubscriptionResponse {
  implicit val format: OFormat[SubscriptionResponse] = Json.format[SubscriptionResponse]
}