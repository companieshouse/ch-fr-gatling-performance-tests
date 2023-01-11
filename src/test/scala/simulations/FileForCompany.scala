package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class FileForCompany extends Simulation {

  val fidcUrl = System.getenv("FIDC_URL") //https://idam-staging.company-information.service.gov.uk/
  val uiUrl = System.getenv("UI_URL") //https://idam-ui-staging.company-information.service.gov.uk/
  val uiAccountUserName = System.getenv("UI_ACCOUNT_USERNAME") //ch-account-ui
  val uiAccountPassword = System.getenv("UI_ACCOUNT_PASSWORD") //N8w8lQCnxs
  val environmentCookieName = System.getenv("ENV_COOKIE_NAME") //8fa4178f20bdf21 for Staging

//  val httpConf = http.baseUrl("https://ewf-stg-aws.companieshouse.gov.uk/")
//    .header("Content-Type", "text/html")

  val httpConf = http.baseUrl(fidcUrl)

  val scn = scenario("File For Company")
    .exec(http("Login Page UI")
      .get(uiUrl + "account/login")
      .header("Accept", "text/html")
      .header("Content-Type", "text/plain")
      .basicAuth(uiAccountUserName, uiAccountPassword)
      .check(status.is(200)))

    .exec(http("Start Login Journey")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHWebFiling-Login")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .check(jsonPath("$.authId").saveAs("authId")))

    .exec(_.set("userName", "loadtest-3@companieshouse.gov.uk"))
    .exec(_.set("password", "L3tM3In1234!"))
    .exec(http("Enter Credentials")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHWebFiling-Login")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .body(ElFileBody("login/enter_credentials.json")).asJson
      .check(jsonPath("$.authId").saveAs("authId"), jsonPath("$.callbacks[3].output[0].value").saveAs("secretOtp")))

    //verify OTP
    .exec(http("Enter OTP")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHWebFiling-Login")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .body(ElFileBody("login/enter_otp.json")).asJson
      .check(jsonPath("$.authId").saveAs("authId")))

    //skip preferences
    .exec(http("Skip Preferences")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHWebFiling-Login")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .body(ElFileBody("login/update_details.json")).asJson
      .check(jsonPath("$.tokenId").saveAs("tokenId")))

    .pause(5)

    //authorize
    .exec(http("Authorize - get Authorization code")
      .get("am/oauth2/authorize?response_type=code&client_id=PerfTestClient&redirect_uri=https://idam-ui.amido.aws.chdev.org/account/home/&scope=openid%20profile%20fr:idm:*%20phone%20email")
      .header("cookie", environmentCookieName + "=${tokenId}")
      .check(status.is(200), currentLocationRegex(".*?code=(.*)&iss=.*").saveAs("returnedCode")))

    .pause(5)

    //test these changes - disable MFA etc. first
    .exec(http("Access Token")
      .post("am/oauth2/realms/root/realms/alpha/access_token")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .header("XXX", "${returnedCode}")
      .header("cookie", environmentCookieName + "=${tokenId}")
      .formParam("grant_type", "authorization_code")
      .formParam("code", "${returnedCode}")
      .formParam("client_id", "PerfTestClient")
      .formParam("client_secret", "Hgn4i8gh11!")
      .formParam("redirect_uri", "https://idam-ui.amido.aws.chdev.org/account/home/")
      .check(jsonPath("$.access_token").saveAs("access_token")))
    .pause(5)

    //hit userinfo endpoint (x4)
    .exec(http("UserInfo")
      .get("am/oauth2/realms/root/realms/alpha/userinfo")
      .header("Authorization", "Bearer ${access_token}")
      .check(status.is(200)))

    .exec(http("UserInfo")
      .get("am/oauth2/realms/root/realms/alpha/userinfo")
      .header("Authorization", "Bearer ${access_token}")
      .check(status.is(200)))

    .exec(http("UserInfo")
      .get("am/oauth2/realms/root/realms/alpha/userinfo")
      .header("Authorization", "Bearer ${access_token}")
      .check(status.is(200)))

    .exec(http("UserInfo")
      .get("am/oauth2/realms/root/realms/alpha/userinfo")
      .header("Authorization", "Bearer ${access_token}")
      .check(status.is(200)))


    //hit company endpoint
    .exec(http("Company Endpoint")
      .get("openidm/endpoint/company?currentPage=1&pageSize=9999&maxPages=10")
      .header("Authorization", "Bearer ${access_token}")
      .check(status.is(200)))

    //Your companies link (UI)
    .exec(http("Your companies UI")
      .get(uiUrl + "account/your-companies")
      .header("Accept", "text/html")
      .header("Content-Type", "text/plain")
      .basicAuth(uiAccountUserName, uiAccountPassword)
      .header("cookies", environmentCookieName + "=${tokenId}")
      .check(status.is(200)))

    .pause(5)

  //File for company times out with 504 currently on Staging - WebFiling needs rebooting if this is the case

  //authorize with OAuth2 client (oidc_client) - Authorization Code flow

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpConf)
}
