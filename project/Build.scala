import sbt._

import Keys._
import AndroidKeys._

object General {
  private def keep(classes:String*) = classes.toList map (c => "-keep class " + c) mkString(" ")

  val settings = Defaults.defaultSettings ++ Seq (
    name := "Remote Runner",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.9.2",
    platformName in Android := "android-15"
  )

  val proguardSettings = Seq (
    useProguard in Android := true//,
   /* proguardOption in Android := keep(
    ) + " -dontskipnonpubliclibraryclassmembers -keepattributes *Annotation*,EnclosingMethod -keepnames class org.codehaus.jackson.** { *; }" +
  "-keep public class local.nodens.remoterunner.** { public void set*(***); public *** get*();  } "
*/
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "change-me",
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.8.RC1" % "test"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "Remote Runner",
    file("."),
    settings = General.fullAndroidSettings
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings ++
               AndroidTest.androidSettings ++
               General.proguardSettings ++ Seq (
      name := "Remote RunnerTests"
    )
  ) dependsOn main
}
