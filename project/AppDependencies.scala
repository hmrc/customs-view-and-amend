import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.25.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "3.34.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % "0.74.0",
    "org.typelevel"     %% "cats-core"                  % "2.9.0",
    "org.jsoup"          % "jsoup"                      % "1.15.3"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "5.25.0"  % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % "0.74.0"  % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8"  % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"   % Test,
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.12" % Test
  )
}
