/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package system.packages

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import common.TestHelpers
import common.Wsk
import common.WskProps
import common.WskTestHelpers
import spray.json.DefaultJsonProtocol.IntJsonFormat
import spray.json.DefaultJsonProtocol.{LongJsonFormat, StringJsonFormat}
import spray.json.pimpAny

/**
 * Tests for alarms trigger service
 */
@RunWith(classOf[JUnitRunner])
class AlarmsFeedTests
    extends FlatSpec
    with TestHelpers
    with WskTestHelpers {

    val wskprops = WskProps()
    val wsk = new Wsk

    behavior of "Alarms trigger service"

    it should "should disable after reaching max triggers" in withAssetCleaner(wskprops) {
        (wp, assetHelper) =>
        implicit val wskprops = wp // shadow global props and make implicit
        val triggerName = s"dummyAlarmsTrigger-${System.currentTimeMillis}"
        val packageName = "dummyAlarmsPackage"

        // the package alarms should be there
        val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
        println("fetched package alarms")
        packageGetResult.stdout should include("ok")

        // create package binding
        assetHelper.withCleaner(wsk.pkg, packageName) {
            (pkg, name) => pkg.bind("/whisk.system/alarms", name)
        }

        // create whisk stuff
        val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName) {
            (trigger, name) =>
            trigger.create(name, feed = Some(s"$packageName/alarm"), parameters = Map(
                    "trigger_payload" -> "alarmTest".toJson,
                    "cron" -> "* * * * * *".toJson,
                    "maxTriggers" -> 3.toJson))
        }
        feedCreationResult.stdout should include("ok")

        // get activation list of the trigger
        val activations = wsk.activation.pollFor(N = 4, Some(triggerName)).length
        println(s"Found activation size: $activations")
        activations should be(3)
    }

    it should "return error message when alarm action does not include cron parameter" in withAssetCleaner(wskprops) {

        (wp, assetHelper) =>
            implicit val wskprops = wp // shadow global props and make implicit
            val triggerName = s"dummyCloudantTrigger-${System.currentTimeMillis}"
            val packageName = "dummyCloudantPackage"
            val feed = "alarm"

            // the package alarms should be there
            val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
            println("fetched package alarms")
            packageGetResult.stdout should include("ok")

            // create package binding
            assetHelper.withCleaner(wsk.pkg, packageName) {
                (pkg, name) => pkg.bind("/whisk.system/alarms", name)
            }

            // create whisk stuff
            val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName, confirmDelete = false) {
                (trigger, name) =>
                    trigger.create(name, feed = Some(s"$packageName/$feed"), parameters = Map(
                        "trigger_payload" -> "alarmTest".toJson),
                        expectedExitCode = 246)
            }
            feedCreationResult.stderr should include("alarms trigger feed is missing the cron parameter")

    }

    it should "return error message when alarms once action does not include date parameter" in withAssetCleaner(wskprops) {

        (wp, assetHelper) =>
            implicit val wskprops = wp // shadow global props and make implicit
            val triggerName = s"dummyCloudantTrigger-${System.currentTimeMillis}"
            val packageName = "dummyCloudantPackage"
            val feed = "once"

            // the package alarms should be there
            val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
            println("fetched package alarms")
            packageGetResult.stdout should include("ok")

            // create package binding
            assetHelper.withCleaner(wsk.pkg, packageName) {
                (pkg, name) => pkg.bind("/whisk.system/alarms", name)
            }

            // create whisk stuff
            val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName, confirmDelete = false) {
                (trigger, name) =>
                    trigger.create(name, feed = Some(s"$packageName/$feed"), parameters = Map(
                        "trigger_payload" -> "alarmTest".toJson),
                        expectedExitCode = 246)
            }
            feedCreationResult.stderr should include("alarms once trigger feed is missing the date parameter")

    }

    it should "return error message when alarm action includes invalid cron parameter" in withAssetCleaner(wskprops) {

        (wp, assetHelper) =>
            implicit val wskprops = wp // shadow global props and make implicit
            val triggerName = s"dummyCloudantTrigger-${System.currentTimeMillis}"
            val packageName = "dummyCloudantPackage"
            val feed = "alarm"

            // the package alarms should be there
            val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
            println("fetched package alarms")
            packageGetResult.stdout should include("ok")

            // create package binding
            assetHelper.withCleaner(wsk.pkg, packageName) {
                (pkg, name) => pkg.bind("/whisk.system/alarms", name)
            }

            val cron = System.currentTimeMillis

            // create whisk stuff
            val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName, confirmDelete = false) {
                (trigger, name) =>
                    trigger.create(name, feed = Some(s"$packageName/$feed"), parameters = Map(
                        "trigger_payload" -> "alarmTest".toJson,
                        "cron" -> cron.toJson),
                        expectedExitCode = 246)
            }
            feedCreationResult.stderr should include(s"cron pattern '${cron}' is not valid")

    }

    it should "return error message when alarms once action includes an invalid date parameter" in withAssetCleaner(wskprops) {

        (wp, assetHelper) =>
            implicit val wskprops = wp // shadow global props and make implicit
            val triggerName = s"dummyCloudantTrigger-${System.currentTimeMillis}"
            val packageName = "dummyCloudantPackage"
            val feed = "once"

            // the package alarms should be there
            val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
            println("fetched package alarms")
            packageGetResult.stdout should include("ok")

            // create package binding
            assetHelper.withCleaner(wsk.pkg, packageName) {
                (pkg, name) => pkg.bind("/whisk.system/alarms", name)
            }

            // create whisk stuff
            val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName, confirmDelete = false) {
                (trigger, name) =>
                    trigger.create(name, feed = Some(s"$packageName/$feed"), parameters = Map(
                        "trigger_payload" -> "alarmTest".toJson,
                        "date" -> "tomorrow".toJson),
                        expectedExitCode = 246)
            }
            feedCreationResult.stderr should include("date parameter 'tomorrow' is not a valid Date")

    }

    it should "return error message when alarms once action date parameter is not a future date" in withAssetCleaner(wskprops) {

        (wp, assetHelper) =>
            implicit val wskprops = wp // shadow global props and make implicit
            val triggerName = s"dummyCloudantTrigger-${System.currentTimeMillis}"
            val packageName = "dummyCloudantPackage"
            val feed = "once"

            // the package alarms should be there
            val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
            println("fetched package alarms")
            packageGetResult.stdout should include("ok")

            // create package binding
            assetHelper.withCleaner(wsk.pkg, packageName) {
                (pkg, name) => pkg.bind("/whisk.system/alarms", name)
            }

            val pastDate = System.currentTimeMillis - 5000

            // create whisk stuff
            val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName, confirmDelete = false) {
                (trigger, name) =>
                    trigger.create(name, feed = Some(s"$packageName/$feed"), parameters = Map(
                        "trigger_payload" -> "alarmTest".toJson,
                        "date" -> pastDate.toJson),
                        expectedExitCode = 246)
            }
            feedCreationResult.stderr should include(s"date parameter '${pastDate}' must be in the future")

    }

    it should "return error message when alarms startDate parameter is not a future date" in withAssetCleaner(wskprops) {

        (wp, assetHelper) =>
            implicit val wskprops = wp // shadow global props and make implicit
            val triggerName = s"dummyCloudantTrigger-${System.currentTimeMillis}"
            val packageName = "dummyCloudantPackage"
            val feed = "alarm"

            // the package alarms should be there
            val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
            println("fetched package alarms")
            packageGetResult.stdout should include("ok")

            // create package binding
            assetHelper.withCleaner(wsk.pkg, packageName) {
                (pkg, name) => pkg.bind("/whisk.system/alarms", name)
            }

            val pastDate = System.currentTimeMillis - 5000

            // create whisk stuff
            val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName, confirmDelete = false) {
                (trigger, name) =>
                    trigger.create(name, feed = Some(s"$packageName/$feed"), parameters = Map(
                        "startDate" -> pastDate.toJson,
                        "cron" -> "* * * * *".toJson),
                        expectedExitCode = 246)
            }
            feedCreationResult.stderr should include(s"startDate parameter '${pastDate}' must be in the future")

    }

    it should "return error message when alarms stopDate parameter is not greater than startDate" in withAssetCleaner(wskprops) {

        (wp, assetHelper) =>
            implicit val wskprops = wp // shadow global props and make implicit
            val triggerName = s"dummyCloudantTrigger-${System.currentTimeMillis}"
            val packageName = "dummyCloudantPackage"
            val feed = "alarm"

            // the package alarms should be there
            val packageGetResult = wsk.pkg.get("/whisk.system/alarms")
            println("fetched package alarms")
            packageGetResult.stdout should include("ok")

            // create package binding
            assetHelper.withCleaner(wsk.pkg, packageName) {
                (pkg, name) => pkg.bind("/whisk.system/alarms", name)
            }

            val stopDate = System.currentTimeMillis + 5000
            val startDate = stopDate

            // create whisk stuff
            val feedCreationResult = assetHelper.withCleaner(wsk.trigger, triggerName, confirmDelete = false) {
                (trigger, name) =>
                    trigger.create(name, feed = Some(s"$packageName/$feed"), parameters = Map(
                        "startDate" -> startDate.toJson,
                        "stopDate" -> stopDate.toJson,
                        "cron" -> "* * * * *".toJson),
                        expectedExitCode = 246)
            }
            feedCreationResult.stderr should include(s"stopDate parameter '${stopDate}' must be greater than the startDate")

    }
}
