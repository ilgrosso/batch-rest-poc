# batch-rest-poc
PoC showing how to implement batch processing via REST with Apache CXF

Inspired by [OData 4.0's Batch requests](http://docs.oasis-open.org/odata/odata/v4.0/os/part1-protocol/odata-v4.0-os-part1-protocol.html#_Toc372793748), this PoC shows how to handle a batch request composed by a sequence of REST requests; compared to OData, the main piece missing are [Change Sets](http://docs.oasis-open.org/odata/odata/v4.0/os/part1-protocol/odata-v4.0-os-part1-protocol.html#_Change_Sets).

## How to run

```
mvn clean verify
```

will spin a Jetty instance bearing the following Apache CXF-based REST services:
* [/users](https://github.com/ilgrosso/batch-rest-poc/blob/master/src/main/java/net/tirasa/batch/rest/poc/api/UserService.java) for common user management
* [/](https://github.com/ilgrosso/batch-rest-poc/blob/master/src/main/java/net/tirasa/batch/rest/poc/api/RootService.java) handling batch

A [test class](https://github.com/ilgrosso/batch-rest-poc/blob/master/src/test/java/net/tirasa/batch/rest/poc/BasicITCase.java) will then send the sample batch request reported below, and verify that the batch response is formatted as expected.

## How does it work?

The actual batch processing is [implemented](https://github.com/ilgrosso/batch-rest-poc/blob/master/src/main/java/net/tirasa/batch/rest/poc/impl/RootServiceImpl.java#L82) as follows:

1. parse the batch request into separate items
1. find matching REST endpoint for each item, delegate actual process and get response
1. build the batch response by assembling all the delegated responses

## Batch requests

The batch request MUST contain a Content-Type header specifying a content type of `multipart/mixed` and a boundary specification as defined in [RFC2046](https://tools.ietf.org/html/rfc2046).

The body of a batch request is made up of a series of individual requests, each represented as a distinct MIME part (i.e. separated by the boundary defined in the `Content-Type` header).

The service MUST process the requests within a batch request sequentially. 

An individual request MUST include a `Content-Type` header with value `application/http` and a `Content-Transfer-Encoding` header with value `binary`.

### Sample request
This contains:
1. user create, with XML payload
1. user update, with JSON payload
1. role delete (no matching REST endpoint for that is available, on purpose)
1. user delete

_Note that ^M represents CR LF_

```
POST /batch HTTP/1.1
Content-Type: multipart/mixed;boundary=batch_4d1ded08-a39f-496b-b341-74730c4d8cdb

--batch_4d1ded08-a39f-496b-b341-74730c4d8cdb^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
POST /users HTTP/1.1
Content-Length: 128
Content-Type: application/xml
^M
<user>
  <username>xxxyyy</username>
  <attributes>
    <firstname>John</firstname>
    <surname>Doe</surname>
  </attributes>
</user>
--batch_4d1ded08-a39f-496b-b341-74730c4d8cdb^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
PUT /users/xxxyyy HTTP/1.1
Content-Length: 94
Content-Type: application/json
^M
{
  "username": "yuyueeee",
  "attributes": [
    "firstname": "Johnny",
    "lastname": "Doe"
  ]
}
--batch_4d1ded08-a39f-496b-b341-74730c4d8cdb^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
DELETE /roles/xxxyyy HTTP/1.1
--batch_4d1ded08-a39f-496b-b341-74730c4d8cdb^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
DELETE /users/xxxyyy HTTP/1.1
--batch_4d1ded08-a39f-496b-b341-74730c4d8cdb--

```

## Batch responses

Requests within a batch are evaluated according to the same semantics used when the request appears outside the context of a batch.

The order of change sets and individual requests in a Batch request is significant. A service MUST process the components of the Batch in the order received.

If the set of request headers of a Batch request are valid (the `Content-Type` is set to `multipart/mixed`, etc.) the service MUST return a `200 OK` HTTP response code to indicate that the request was accepted for processing.

If the service receives a Batch request with an invalid set of headers it MUST return a `4xx response` code and perform no further processing of the request.

A response to a batch request MUST contain a `Content-Type` header with value `multipart/mixed`.

Structurally, a batch response body MUST match one-to-one with the corresponding batch request body, such that the same multipart MIME message structure defined for requests is used for responses

### Sample response
_Note that ^M represents CR LF_

```
HTTP/1.1 200 Ok
Content-Type: multipart/mixed;boundary=batch_80ce758a-775e-48c8-9c32-42d9bb0c4ef2

--batch_80ce758a-775e-48c8-9c32-42d9bb0c4ef2^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 201 Created
Date: Tue, 24 Jul 2018 08:31:22 GMT
Content-Type: application/xml
^M
<user>
  <username>xxxyyy</username>
  <attributes>
    <firstname>John</firstname>
    <surname>Doe</surname>
  </attributes>
</user>

--batch_80ce758a-775e-48c8-9c32-42d9bb0c4ef2^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 200 OK
Date: Tue, 24 Jul 2018 08:31:22 GMT
Content-Type: application/json
^M
{
  "username": "yuyueeee",
  "attributes": [
    "firstname": "Johnny",
    "lastname": "Doe"
  ]
}

--batch_80ce758a-775e-48c8-9c32-42d9bb0c4ef2^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 404 Not Found
Content-Length: 0
Date: Tue, 24 Jul 2018 08:31:22 GMT
Allow: DELETE,POST,GET,PATCH,PUT,OPTIONS,HEAD
^M
--batch_80ce758a-775e-48c8-9c32-42d9bb0c4ef2^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 204 No Content
Content-Length: 0
Date: Tue, 24 Jul 2018 08:31:22 GMT
^M
--batch_80ce758a-775e-48c8-9c32-42d9bb0c4ef2--

```
