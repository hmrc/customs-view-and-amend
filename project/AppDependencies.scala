import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootstrapFrontendPlayVersion = "7.22.0"
  val hmrcMongoPlayVersion         = "1.3.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapFrontendPlayVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "7.23.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % hmrcMongoPlayVersion,
    "org.typelevel"     %% "cats-core"                  % "2.10.0",
    "org.jsoup"          % "jsoup"                      % "1.16.1"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapFrontendPlayVersion % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoPlayVersion         % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8"                     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"                      % Test,
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.12"                    % Test
  )
}
