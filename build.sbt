ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11" // This needs to be changed to Scala 2.12 for Spark compatibility

lazy val root = (project in file("."))
  .settings(
    name := "HW2_Code"
  )

scalacOptions += "-Ytasty-reader"

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.17.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.17.1",
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",
  "org.apache.hadoop" % "hadoop-common" % "3.2.4",
  "org.apache.hadoop" % "hadoop-hdfs" % "3.2.4",
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.2.4",

  // Spark dependencies
  "org.apache.spark" %% "spark-core" % "3.3.1" ,
  "org.apache.spark" %% "spark-sql" % "3.3.1",
  "org.apache.spark" %% "spark-graphx" % "3.3.1",

)

unmanagedJars in Compile ++= Seq(
  file("lib/netmodelsim.jar")
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => xs match {
    case "MANIFEST.MF" :: Nil => MergeStrategy.discard
    case "LICENSE" :: Nil => MergeStrategy.discard
    case "NOTICE" :: Nil => MergeStrategy.discard
    case "DEPENDENCIES" :: Nil => MergeStrategy.discard
    case "services" :: _ => MergeStrategy.concat
    case ps @ (x :: _) if ps.last.endsWith(".SF") || ps.last.endsWith(".DSA") || ps.last.endsWith(".RSA") => MergeStrategy.discard
    case "spring.schemas" :: Nil => MergeStrategy.concat
    case "spring.handlers" :: Nil => MergeStrategy.concat
    case _ => MergeStrategy.first
  }
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

