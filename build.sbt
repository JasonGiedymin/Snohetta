name := "Snohetta"

version := "0.2"

scalaVersion := "2.10.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "OSS Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers ++= Seq("Twitter Repo" at "http://maven.twttr.com/")

libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
)

// For Eclipse sources otherwise would have to do 'sbt update classifiers'
// which doesn't always (if at anytime) really work in a locked network...
EclipseKeys.withSource := true

javacOptions ++= Seq("-encoding", "UTF-8")
