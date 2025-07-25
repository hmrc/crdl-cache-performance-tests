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
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import uk.gov.hmrc.performance.conf.ServicesConfiguration

object CRDLCacheRequests extends ServicesConfiguration {

  val baseUrl: String = s"${baseUrlFor("crdl-cache")}/crdl-cache"

  val fetchCountries: HttpRequestBuilder =
    http("Fetch Countries")
      .get(s"$baseUrl/lists/BC08")
      .check(status.is(200))
      .check(jsonPath("$").count.gt(0))

  val fetchMemberStates: HttpRequestBuilder =
    http("Fetch Member States")
      .get(s"$baseUrl/lists/BC11")
      .check(status.is(200))
      .check(jsonPath("$").count.gt(0))

  val fetchPackagingTypes: HttpRequestBuilder =
    http("Fetch Packaging Types")
      .get(s"$baseUrl/lists/BC17")
      .check(status.is(200))
      .check(jsonPath("$").count.gt(0))

  val fetchTransportUnits: HttpRequestBuilder =
    http("Fetch Transport Units")
      .get(s"$baseUrl/lists/BC35")
      .check(status.is(200))
      .check(jsonPath("$").count.gt(0))

  val fetchWineOperations: HttpRequestBuilder =
    http("Fetch Wine Operations")
      .get(s"$baseUrl/lists/BC41")
      .check(status.is(200))
      .check(jsonPath("$").count.gt(0))

  val fetchDocumentTypes: HttpRequestBuilder =
    http("Fetch Document Types")
      .get(s"$baseUrl/lists/BC106")
      .check(status.is(200))
      .check(jsonPath("$").count.gt(0))
}
