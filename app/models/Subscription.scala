package models

import play.api.libs.json.{Json, OFormat}

case class Subscription (
                          eoriGB: String,
                          eoriXI: String
                        )


object Subscription{
  implicit val format: OFormat[Subscription] = Json.format[Subscription]
}