"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

app.controller('DemoAppController', function($http, $location) {
    const demoApp = this;

    const apiBaseURL = "/";

    demoApp.getTokens = () => $http.get(apiBaseURL + "tokens")
        .then((response) => demoApp.tokens = Object.keys(response.data)
            .map((key) => response.data[key])
            .reverse());

    demoApp.getAssets = () => $http.get(apiBaseURL + "assets")
        .then((response) => demoApp.assets = Object.keys(response.data)
            .map((key) => response.data[key])
            .reverse());

    demoApp.getTokens();
    demoApp.getAssets();
});
