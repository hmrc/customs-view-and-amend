import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val bootstrapFrontendPlayVersion = "9.16.0"
  val hmrcMongoPlayVersion         = "2.6.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapFrontendPlayVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "12.7.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoPlayVersion,
    "org.typelevel"     %% "cats-core"                  % "2.13.0",
    "org.jsoup"          % "jsoup"                      % "1.19.1"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapFrontendPlayVersion % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoPlayVersion         % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.8"                     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1"                      % Test,
    "org.scalamock"          %% "scalamock"               % "6.0.0"                      % Test
  )
}
