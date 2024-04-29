
# customs-view-and-amend

Frontend microservice providing an HTML UI for checking the status of claims for repayments on overpayments for import charges.

- [Business goal](#business-goal)
- [Usage](#usage)
- [Internals](#internal)
- [External connections](#external-connections)
- [Feature flags](#feature-flags)
- [Development](#development)

## Business goal

You can use this service to view the progress/status of all the claims submitted by the users on:
 - overpayments for import charges, these include Customs Duties, Excise Duties, Countervailing Duties and specific other customs duties,
 - rejected goods, if the import is  rejected  as the goods are damaged, defective, or not by contract
 - security deposits

## Usage

This service is publicly available under <https://www.tax.service.gov.uk/claim-back-import-duty-vat/claims-status> URL. 

Access to this service requires:
 - [Government Gateway login](https://www.gov.uk/log-in-register-hmrc-online-services)
 - [Customs Declaration Service subscription](https://www.gov.uk/guidance/get-access-to-the-customs-declaration-service)

## Internals

Internally the service consists of several types of components:
 - http endpoints defined in `conf/*.routes` files
 - action controllers classes
 - session cache repository
 - data model classes
 - forms definitions
 - html templates written in Twirl language
 - third-party http services connectors

## External connections

This service calls the following external services:
 - [CDS-Reimbursement backend microservice](https://github.com/hmrc/cds-reimbursement-claim) for:
   - claims list (TPI01),
   - claim details (TPI02),
   - CDS subscription details e.g. email, XI EORI (SUB09),
   - document submission (DEC64)
- Auth microservice for authentication and authorisation of the users,
- [Upload customs documents frontend](https://github.com/hmrc/upload-customs-documents-frontend) for uploading the evidence.

## Feature flags

The feature set of the service is controlled by a host of feature flags defined in the [conf/application.conf](https://github.com/hmrc/customs-view-and-amend/blob/main/conf/application.conf#L170-L175):

| flag | description |
|------|-------------|
| features.welsh-language-support | enables welsh language selector |
| features.report-a-problem | enables problem reporting link |
| features.include-xi-claims | enables support for displaying declarations containing XI EORI identifiers |
| features.limited-access | enables access only for users on the an allow list of EORIs defined in th `limited-access-eori-csv-base64` property |

## Development

This service is built using [Play Framework](https://www.playframework.com/) and Scala language (https://www.scala-lang.org/).

### Prerequisites
 - [Java 21](https://adoptium.net/)
 - [SBT build tool](https://www.scala-sbt.org/)

### Build and test

    sbt clean compile test

### Run locally

Running this service locally requires multiple other services to be up and running. The best way to achieve that is by using [Service Manager](https://github.com/hmrc/sm2):

    sm --start CDSRC_ALL

### Run locally in a development mode

    sm --start CDSRC_ALL
    sm --stop CUSTOMS_VIEW_AND_AMEND
    sbt run

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

