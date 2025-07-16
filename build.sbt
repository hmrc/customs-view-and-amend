import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "customs-view-and-amend"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("fix", "all compile:scalafix test:scalafix")

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := List(
      "<empty>",
      "Reverse.*",
      ".*views.*",
      ".*config.*",
      ".*helpers.*",
      ".*(BuildInfo|Routes|testOnly).*"
    ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtDistributablesPlugin
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(scalafmtOnCompile := true)
  .settings(scoverageSettings: _*)
  .settings(PlayKeys.playDefaultPort := 9399)
  .settings(Assets / pipelineStages := Seq(uglify))
  .settings(uglifyOps := UglifyOps.singleFile)
  .settings(
    majorVersion := 0,
    scalaVersion := "3.3.6",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += s"-Wconf:src=${target.value}/scala-${scalaBinaryVersion.value}/routes/.*:s,src=${target.value}/scala-${scalaBinaryVersion.value}/twirl/.*:s"
  )
