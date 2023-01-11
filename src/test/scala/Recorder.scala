package uk.gov.companieshouse
package test.scala

import io.gatling.recorder.GatlingRecorder
import io.gatling.recorder.config.RecorderPropertiesBuilder

object Recorder extends App {

  val props = new RecorderPropertiesBuilder()
    .simulationsFolder(IDEPathHelper.mavenSourcesDirectory.toString)
    .resourcesFolder(IDEPathHelper.mavenResourcesDirectory.toString)
    .simulationPackage("uk.gov.companieshouse.gatlingTest")

  GatlingRecorder.fromMap(props.build, Some(IDEPathHelper.recorderConfigFile))
}