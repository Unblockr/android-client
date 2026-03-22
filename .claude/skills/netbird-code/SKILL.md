---
name: netbird-code
description: Creates or edits code to communicate with Netbird management server API.
author: Mark
---

Create or edit code to communicate with Netbird management server API. The server API is documented at https://docs.netbird.io/api. You can use the API to manage devices, users, and other resources in a Netbird network.
The app will be communicating with the Netbird management server API using HTTP requests. You can use any programming language that supports making HTTP requests, such as Python, JavaScript, or Go.
When creating or editing code to communicate with the Netbird management server API the app should assume that the server is running at https://netbird.unblockr.net/api.
Upon registration, the app will receive an id key that should be used to identify the peer. This key should be used in the Authorization header of all API requests to authenticate the peer. The app should also store the id key securely for future use.
The app should also handle any errors that may occur when making API requests, such as network errors or authentication errors. The app should provide appropriate error messages to the user and allow them to retry the request if necessary.
When using an API call, you must make a note in @README.md that you have used the API call and provide a brief description of what the API call does. This will help other developers understand how the app is communicating with the Netbird management server API and what functionality it provides. It will also allow for the possiblility of a serverless shim to be created in the future that can be used to test the app without needing to connect to the actual Netbird management server API and to function as a proxy for the actual Netbird management server API to protect the API keys and to provide additional functionality such as caching or rate limiting.