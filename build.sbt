import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._

import scalariform.formatter.preferences._

val commonSettings = Seq(
  organization := "org.zalando",
  version := "0.2",
  scalaVersion := "2.11.7",
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  resolvers := Seq(
    "scalaz-bintray"    at "http://dl.bintray.com/scalaz/releases",
    "scoverage-bintray" at "https://dl.bintray.com/sksamuel/sbt-plugins/"
  )
)

val playFrameworkVersion = "2.5.1"

lazy val testDependencies =
  Seq(
    "org.specs2" %% "specs2-core" % "3.6.4" % "test",
    "org.specs2" %% "specs2-junit" % "3.6.4" % "test"
  )

lazy val playDependencies =
  Seq(
    "com.typesafe.play" %% "play-json" % playFrameworkVersion,
    "com.typesafe.play" %% "play-ws" % playFrameworkVersion,
    "com.typesafe.play" %% "play" % playFrameworkVersion,
    "com.typesafe.play" %% "play-test" % playFrameworkVersion % "test",
    "com.typesafe.play" %% "play-specs2" % playFrameworkVersion % "test"
  )


lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "play-Zhewbacca")
  .settings(version := "0.1.0")
  .settings(libraryDependencies ++= (testDependencies ++ playDependencies))
  .settings(parallelExecution in Test := false)

// Define a special task which does not fail when any publish task fails for any module,
// so repeated publishing will be performed no matter the previous publish result.

// this checks violations to the paypal style guide
// copied from https://raw.githubusercontent.com/paypal/scala-style-guide/develop/scalastyle-config.xml
scalastyleConfig := file("scalastyle-config.xml")

scalastyleFailOnError := true

// Create a default Scala style task to run with tests
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compileInputs in(Compile, compile)) <<= (compileInputs in(Compile, compile)) dependsOn compileScalastyle

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  .setPreference(PreserveSpaceBeforeArguments, false)
  .setPreference(AlignSingleLineCaseStatements, false)
  .setPreference(IndentLocalDefs, true)
  .setPreference(IndentPackageBlocks, true)
  .setPreference(IndentWithTabs, false)
  .setPreference(IndentSpaces, 2)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
  .setPreference(SpacesAroundMultiImports, false)

pomExtra := (
  <url>https://github.com/zalando-incubator/play-zhewbacca</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>https://raw.githubusercontent.com/zalando-incubator/play-zhewbacca/master/LICENSE</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:zalando-incubator/play-zhewbacca.git</url>
      <connection>scm:git:git@github.com:zalando-incubator/play-zhewbacca.git</connection>
    </scm>
    <developers>
      <developer>
        <name>Dmitry Krivaltsevich</name>
      </developer>
      <developer>
        <name>William Okuyama</name>
      </developer>
    </developers>)