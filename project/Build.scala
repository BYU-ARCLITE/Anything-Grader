import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "anythingGrader"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.apache.opennlp" % "opennlp-tools" % "1.5.2-incubating",
      "org.apache.commons" % "commons-lang3" % "3.1",
      "commons-io" % "commons-io" % "2.4",
      "mysql" % "mysql-connector-java" % "5.1.10",
      "com.google.gdata.gdata-java-client" % "gdata-client-1.0" % "1.47.0",
      "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.2"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here
      resolvers += "OpenNLP Repository" at "http://opennlp.sourceforge.net/maven2/",
      resolvers += "Google Gdata Maven Repository" at "http://maven.burtsev.net/"
    )

}
