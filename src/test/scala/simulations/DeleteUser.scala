package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class DeleteUser extends Simulation {

  val fidcUrl = System.getenv("FIDC_URL")
  var accessToken = System.getenv("ACCESS_TOKEN")

  val httpConf = http.baseUrl(fidcUrl)

  val userName = "loadtest-6@companieshouse.gov.uk"

  // Get access token from forgerock-cloud-config/bash-scripts/get_idm_access_token.sh
  accessToken = "eyJ0eXAiOiJKV1QiLCJraWQiOiJEZTF4MjErUEJpTWREODZMQUJoWXZ3UEY3cmM9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJkYTI5YTM1OS0wZGU1LTQ3NjgtYThkZi0yOWU0NmU4MTI1ZDkiLCJjdHMiOiJPQVVUSDJfU1RBVEVMRVNTX0dSQU5UIiwiYXV0aF9sZXZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiM2Y2Y2VkMmItNmE5MC00YThkLTgxMmQtMjIxZjg0ZjMyNTkzLTYwNzIzNzMzIiwic3VibmFtZSI6ImRhMjlhMzU5LTBkZTUtNDc2OC1hOGRmLTI5ZTQ2ZTgxMjVkOSIsImlzcyI6Imh0dHBzOi8vb3BlbmFtLWNvbXBhbmllc2hvdXNlLXVrLWRldi5pZC5mb3JnZXJvY2suaW86NDQzL2FtL29hdXRoMiIsInRva2VuTmFtZSI6ImFjY2Vzc190b2tlbiIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJhdXRoR3JhbnRJZCI6IlgzUXNYUnFIdlYxVVpsRVJWZm9hWFZUUjFGYyIsImF1ZCI6ImlkbUFkbWluQ2xpZW50IiwibmJmIjoxNjczMjc1Mjg4LCJncmFudF90eXBlIjoiYXV0aG9yaXphdGlvbl9jb2RlIiwic2NvcGUiOlsiZnI6aWRtOioiXSwiYXV0aF90aW1lIjoxNjczMjc1Mjg1LCJyZWFsbSI6Ii8iLCJleHAiOjE2NzMyNzg4ODgsImlhdCI6MTY3MzI3NTI4OCwiZXhwaXJlc19pbiI6MzYwMCwianRpIjoiR0VTYWxxOEZpejUxM1ExNlR2MzdseEFjMmRRIn0.jlT0M3PbAJ0csGy9dtzGb6dzbssg4tBuHUscLrxRYLGLoXRv7ppp_QC2J1lVsDVLm5GhRtwXMrYIpdbVaSaRdZGrZEEVR-HiRIwrPvuxEWfUzcrTv_hRsG2uJJqVURJDy8iO__XE3nnaZeDP-qgwK_PdFvn50IED2BydUjsxAkTC6UNf7Eq6DzHrOcMNu0ZGlbDy_cIZ41Ge85D8WqBkC334VgtL67AulOVNe_rcBvti5UxkeCp8_rDogN9O3v-4kJ4aBCn1ERJ4Pre95iv6-ZhAFMHnOH0G1MSvL4dpAROblLB3Zm9s2sngndXM_bszqDZpVmNYVh3qDAZChKVQEg"

  val scn = scenario("Delete User")
    // Set username below from input
    .exec(http("Get User ID")
      .get("openidm/managed/alpha_user?_queryFilter=userName+eq+\"" + userName + "\"&_pageSize=10&_fields=userName")
      .header("Authorization", "Bearer " + accessToken)
      .header("Accept-API-Version", "resource=1.0")
      .check(jsonPath("$.result[0]._id").saveAs("userId")))

    .exec(http("Delete User")
      .delete("openidm/managed/alpha_user/${userId}")
      .header("Authorization", "Bearer " + accessToken)
      .header("Accept-API-Version", "resource=1.0")
      .check(status.is(200)))

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpConf)
}
