package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

class RegisterUser extends Simulation {

  val fidcUrl = System.getenv("FIDC_URL")
  val uiUrl = System.getenv("UI_URL")
  val uiAccountUserName = System.getenv("UI_ACCOUNT_USERNAME")
  val uiAccountPassword = System.getenv("UI_ACCOUNT_PASSWORD")
  val environmentCookieName = System.getenv("ENV_COOKIE_NAME")

  // 1 Http Conf
  val httpConf = http.baseUrl(fidcUrl)
    .header("Accept-API-Version", "resource=1.0, protocol=2.1")
    .header("Content-Type", "application/json")

  // 2 Scenario Definition
  val scn = scenario("Register User")
    .exec(http("Register")
      .get(uiUrl + "account/register/_start")
      .header("Accept", "text/html")
      .header("Content-Type", "text/plain")
      .basicAuth(uiAccountUserName, uiAccountPassword)
      .check(status.is(200)))

    .exec(http("Start Registration Journey")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHRegistration")
      .check(jsonPath("$.authId").saveAs("authId")))

    .exec(_.set("creationName", "LoadTestUser"))
    .exec(_.set("creationEmail", "loadtest-6@companieshouse.gov.uk"))
    .exec(http("Confirm Answers")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHRegistration")
      .body(ElFileBody("register/check-your-answers.json")).asJson
      .check(jsonPath("$.authId").saveAs("authId")))

    .exec(http("Send Name and Email Address")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHRegistration")
      .body(ElFileBody("register/send_username_and_email.json")).asJson
      .check(jsonPath("$.authId").saveAs("authId"),jsonPath("$.callbacks[4].output[0].value").saveAs("registrationJwt")))

    .pause(5)

  //Start Verify journey
    .exec(http("Start Verify Journey from Email Link")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHVerifyReg&token=${registrationJwt}")
      .check(jsonPath("$.authId").saveAs("authId")))

  //Enter password twice
    .exec(_.set("creationPassword","L3tM3In1234!"))
    .exec(http("Verify Select Password")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHVerifyReg&token=${registrationJwt}")
      .body(ElFileBody("register/enter_password_twice.json")).asJson
      .check(jsonPath("$.authId").saveAs("authId")))

    .pause(5)

  //Marketing preferences
    .exec(http("Marketing Preferences")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHVerifyReg&token=${registrationJwt}")
      .body(ElFileBody("register/choose_marketing_preferences.json")).asJson
      .check(jsonPath("$.tokenId").saveAs("tokenId")))

    .pause(5)

  //Logout
    .exec(http("Logout")
      .post("am/json/realms/root/realms/alpha/sessions/?_action=logout")
      .header("Accept-API-Version", "resource=3.1, protocol=1.0")
      .header(environmentCookieName, "${tokenId}")
      .check(jsonPath("$.result").saveAs("result")))

  // 3 Load Scenario
  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpConf)

}