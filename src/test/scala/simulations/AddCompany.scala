package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class AddCompany extends Simulation {

  val fidcUrl = System.getenv("FIDC_URL")
  val uiUrl = System.getenv("UI_URL")
  val uiAccountUserName = System.getenv("UI_ACCOUNT_USERNAME")
  val uiAccountPassword = System.getenv("UI_ACCOUNT_PASSWORD")
  val environmentCookieName = System.getenv("ENV_COOKIE_NAME")
  val perfTestClientSecret = System.getenv("CLIENT_SECRET")

  val httpConf = http.baseUrl(fidcUrl)

  //login
  val scn = scenario("Add Company")
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
      .formParam("client_secret", perfTestClientSecret)
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

  //Add company (x50?)
    .exec(http("Add a company UI")
      .get(uiUrl + "account/associate/_start")
      .header("Accept", "text/html")
      .header("Content-Type", "text/plain")
      .basicAuth(uiAccountUserName, uiAccountPassword)
      .header("cookies", environmentCookieName + "=${tokenId}")
      .check(status.is(200)))

    .pause(5)

    .exec(http("Start Add Company")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHCompanyAssociation")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .header("cookie", environmentCookieName + "=${tokenId}")
      .check(status.is(200), jsonPath("$.authId").saveAs("authId")))

    .exec(_.set("companyNumber", "00002065"))
    .exec(_.set("companyName", "LLOYDS BANK PLC 1"))
    .exec(http("Enter Company Details")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHCompanyAssociation")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .header("cookie", environmentCookieName + "=${tokenId}")
      .body(ElFileBody("login/enter_company_details.json")).asJson
      .check(status.is(200), jsonPath("$.authId").saveAs("authId")))

    .exec(http("Confirm Company")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHCompanyAssociation")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .header("cookie", environmentCookieName + "=${tokenId}")
      .body(ElFileBody("login/confirm_company_details.json")).asJson
      .check(jsonPath("$.authId").saveAs("authId")))

    .exec(_.set("authCode", "222222"))
    .exec(http("Enter Auth Code")
      .post("am/json/realms/root/realms/alpha/authenticate?authIndexType=service&authIndexValue=CHCompanyAssociation")
      .header("Accept-API-Version", "resource=1.0, protocol=2.1")
      .header("Content-Type", "application/json")
      .header("cookie", environmentCookieName + "=${tokenId}")
      .body(ElFileBody("login/enter_auth_code.json")).asJson
      .check(jsonPath("$.authId").saveAs("authId")))

    pause(5)

    .exec(http("Company Endpoint")
      .get("openidm/endpoint/company?currentPage=1&pageSize=9999&maxPages=10")
      .header("Authorization", "Bearer ${access_token}")
      .check(status.is(200)))

  //Logout
    .exec(http("Logout")
      .post("am/json/realms/root/realms/alpha/sessions/?_action=logout")
      .header("Accept-API-Version", "resource=3.1, protocol=1.0")
      .header(environmentCookieName, "${tokenId}")
      .check(jsonPath("$.result").saveAs("result")))

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpConf)
}
