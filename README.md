<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Hackathon Template

This is a simple template app that defines:

* Two simple states (under `cordapp-contracts-states/src/main/java/`):
    * `TokenState` defines a simple token
    * `AssetState` defines a generic asset
* Two simple contracts governing the above (under `cordapp-contracts-states/src/main/java/`):
    * `TokenContract`
    * `AssetContract`
* Three flows (under `cordapp/src/main/java/`):
    * `IssueToken` (args `recipient` and `amount`) to issue a token
    * `IssueAsset` (args `recipient` and `description`) to issue an asset
    * `ExchangeAssetForTokenInitiator` (args `assetId`, `counterparty` and `amount`) to trade one of your assets for 
       someone else's tokens
        * For the trade to be successful, the counterparty must have a token worth exactly the quoted price
* A UI for interacting with the nodes (under `clients/src/main/java/`)

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Running the webserver

Run the following Gradle commands to start the webservers once the nodes are started:

* `gradlew runPartyAServer` to create a server connected to PartyA on `localhost:50005`
* `gradlew runPartyBServer` to create a server connected to PartyB on `localhost:50006`
* `gradlew runPartyCServer` to create a server connected to PartyC on `localhost:50007`

## Interacting with the webserver

Each server has a GUI at `/`.

It also has the following endpoints:

* `GET /tokens` to see a list of owned tokens
* `GET /assets` to see a list of owned assets
* `POST /issue-token` with params `recipient` and `amount` to issue an token
* `POST /issue-asset` with params `recipient` and `description` to issue an asset
* `POST /exchange-asset-for-token` with params `assetId`, `counterparty` and `amount` to exchange one of your assets 
   for someone else's tokens
