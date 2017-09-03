name := """chat-with-scala"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(guice,
  "com.typesafe.akka" %% "akka-cluster" % "2.5.4",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test)
