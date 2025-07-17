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
import io.gatling.core.action.builder.PauseBuilder
import uk.gov.hmrc.performance.conf.ServicesConfiguration
import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import uk.gov.hmrc.perftests.example.CRDLCacheRequests._

import scala.concurrent.duration._

class CRDLCacheSimulation extends PerformanceTestRunner with ServicesConfiguration {
  val crdlCacheUrl   = s"${baseUrlFor("crdl-cache")}/crdl-cache"
  val emcsRefDataUrl = s"${baseUrlFor("emcs-tfe-crdl-reference-data")}/emcs-tfe-reference-data"

  def deleteCrdlData(entity: String) = {
    requests.delete(s"$crdlCacheUrl/test-only/$entity")
  }

  def importCrdlCata(entity: String) = {
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
      importCrdlCata("codelists")
      importCrdlCata("correspondence-lists")
      // Wait for the imports to complete
      while (getCrdlImportStatus("codelists") != "IDLE") {
        Thread.sleep(200)
      }
      while (getCrdlImportStatus("correspondence-lists") != "IDLE") {
        Thread.sleep(200)
      }
    }
  }

  val pause = new PauseBuilder(1.second, None)

  setup("create-draft-movement", "Create A Draft Movement").withActions(
    // Countries and member states are fetched at the same time
    fetchCountries,
    fetchMemberStates,
    pause,
    fetchPackagingTypes,
    pause,
    fetchTransportUnits,
    pause,
    fetchWineOperations,
    pause,
    fetchDocumentTypes
  )

  runSimulation()
}
