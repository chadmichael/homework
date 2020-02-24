ThisBuild / scalaVersion := "2.12.3"
ThisBuild / organization := "com.blackdog" 

val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
val scalaArm = "com.jsuereth" %% "scala-arm" % "2.0"

lazy val netflowprocessor = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "netflowprocessor",
    libraryDependencies += scalaArm,
    libraryDependencies += scalaTest % Test,
  )
