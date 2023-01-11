# ForgeRock Identity Cloud Gatling Performance Tests

This repository contains Gatling Performance Tests for the Companies House WebFiling/IDAM solution.

Currently these can be run locally but will run as part of a pipeline in the future.

The tests should be ideally run against Staging with the `forgerock-cloud-config` `load-testing` branch deployed to ensure 
the OTP is returned in the JSON resposne and Notify emails are disabled

## Running Locally

### Pre-Requisites

The following need to be installed/configured for local use:

- Java
- Maven

### Environment Variables

A `.env` file can be used for setting environment variables when running locally. Copy the `.env.sample` file to a new file called `.env` and update the values for the environment.

| Name                | Description                            | Default Value | Required           |
|---------------------|----------------------------------------| ------------- |--------------------|
| FIDC_URL            | ForgeRock Identity Cloud URL           | N/A           | :white_check_mark: |
| UI_URL              | CH Account UI URL                      | N/A           | :white_check_mark: |
| ENV_COOKIE_NAME     | Environment cookie name                | N/A           | :white_check_mark: |
| UI_ACCOUNT_USERNAME | UI Staging username for login over VPN | N/A           |                    |
| UI_ACCOUNT_PASSWORD | UI Staging password for login over VPN | N/A           |                    |
| CLIENT_SECRET       | Client secret for the PerfTestClient   | N/A           | :white_check_mark: |

### Run the tests

The tests can be run within an IDE by running `uk.gov.companieshouse.test.scala.Engine` class and with the environment variables set.

Alternatively from the command line by running `mvn gatling:test`