ThisBuild / scalaVersion := "2.12.3"


lazy val netflowprocessor = (project in file("."))
  .settings(
    name := "netflowprocessor",
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  )
