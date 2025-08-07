/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.perftests.example

import io.gatling.core.Predef._
import uk.gov.hmrc.performance.conf.ServicesConfiguration
import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import uk.gov.hmrc.perftests.example.CRDLCacheRequests._

import scala.util.Random

class CRDLCacheSimulation extends PerformanceTestRunner with ServicesConfiguration {
  val internalAuthUrl   = s"${baseUrlFor("internal-auth")}"
  val crdlCacheUrl      = s"${baseUrlFor("crdl-cache")}/crdl-cache"
  val internalAuthToken = if (runLocal) "crdl-cache-token" else sys.env("INTERNAL_AUTH_TOKEN")

  val packagingTypes =
    csv("data/packaging-types.csv").readRecords
      .map(_("packagingType").toString)

  val randomPackagingTypes = Iterator.continually {
    Map("packagingTypes" -> Random.shuffle(packagingTypes).take(1 + Random.nextInt(6)))
  }

  val wineOperations =
    csv("data/wine-operations.csv").readRecords
      .map(_("wineOperation").toString)

  val randomWineOperations = Iterator.continually {
    Map("wineOperations" -> Random.shuffle(wineOperations).take(1 + Random.nextInt(4)))
  }

  // Random lists of packaging types for the packaging types endpoint
  def packagingTypesFeeder = feed(randomPackagingTypes)

  // Random lists of wine operations for the wine operations endpoint
  def wineOperationsFeeder = feed(randomWineOperations)

  def internalAuthTokenExists() = {
    requests
      .get(
        s"$internalAuthUrl/test-only/token",
        headers = Map("Authorization" -> internalAuthToken),
        check = false
      )
      .statusCode == 200
  }

  def createInternalAuthToken() = {
    requests.post(
      s"$internalAuthUrl/test-only/token",
      headers = Map("Content-Type" -> "application/json"),
      data = ujson.Obj(
        "token"     -> internalAuthToken,
        "principal" -> "performance-jenkins",
        "permissions" -> ujson.Arr(
          ujson.Obj(
            "resourceType"     -> "crdl-cache",
            "resourceLocation" -> "*",
            "actions"          -> ujson.Arr("READ")
          )
        )
      )
    )
  }

  def deleteCrdlData(entity: String) = {
    requests.delete(s"$crdlCacheUrl/test-only/$entity")
  }

  def importCrdlData(entity: String) = {
    requests.post(s"$crdlCacheUrl/test-only/$entity")
  }

  def getCrdlImportStatus(entity: String) = {
    val response = requests.get(s"$crdlCacheUrl/test-only/$entity")
    val json     = ujson.read(response)
    json("status").str
  }

  if (runLocal) {
    before {
      // Clear down any existing data
      deleteCrdlData("last-updated")
      deleteCrdlData("codelists")
      deleteCrdlData("correspondence-lists")
      // Import everything again
      importCrdlData("codelists")
      importCrdlData("correspondence-lists")
      // Wait for the imports to complete
      while (getCrdlImportStatus("codelists") != "IDLE") {
        Thread.sleep(200)
      }
      while (getCrdlImportStatus("correspondence-lists") != "IDLE") {
        Thread.sleep(200)
      }
      if (!internalAuthTokenExists()) {
        createInternalAuthToken()
      }
    }
  }

  setup("fetch-packaging-types", "Fetch Packaging Types")
    .withActions(fetchPackagingTypes)

  setup("fetch-member-states", "Fetch Member States")
    .withActions(fetchMemberStates)

  setup("fetch-countable-packaging-types", "Fetch Countable Packaging Types")
    .withActions(fetchCountablePackagingTypes)

  setup("fetch-document-types", "Fetch Document Types")
    .withActions(fetchDocumentTypes)

  setup("fetch-specified-packaging-types", "Fetch Specified Packaging Types")
    .withActions(packagingTypesFeeder.actionBuilders: _*)
    .withActions(fetchSpecifiedPackagingTypes)

  setup("fetch-specified-wine-operations", "Fetch Specified Wine Operations")
    .withActions(wineOperationsFeeder.actionBuilders: _*)
    .withActions(fetchSpecifiedWineOperations)

  setup("fetch-member-states-and-countries", "Fetch Member States And Countries")
    .withActions(fetchMemberStates, fetchCountries)

  runSimulation()
}
