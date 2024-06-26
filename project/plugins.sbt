resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)
resolvers += Resolver.typesafeRepo("releases")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"     % "3.21.0")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables" % "2.5.0")
addSbtPlugin("org.playframework"  % "sbt-plugin"         % "3.0.2")
addSbtPlugin("com.typesafe.sbt"   % "sbt-gzip"           % "1.0.2")
addSbtPlugin("io.github.irundaia" % "sbt-sassify"        % "1.5.2")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"      % "2.0.11")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"       % "0.12.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-uglify"         % "2.0.0")
