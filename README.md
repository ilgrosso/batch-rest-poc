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

A [test class](https://github.com/ilgrosso/batch-rest-poc/blob/master/src/test/java/net/tirasa/batch/rest/poc/BasicITCase.java) will then send the sample batch request reported below, and verify that the batch response is formatted as expected, both with synchronous and asynchronous processing.

## How does it work?

The actual batch processing is [implemented](https://github.com/ilgrosso/batch-rest-poc/blob/master/src/main/java/net/tirasa/batch/rest/poc/impl/RootServiceImpl.java#L94) as follows:

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
1. user delete with bad key
1. user delete

_Note that ^M represents CR LF_

```
POST /batch HTTP/1.1
Content-Type: multipart/mixed;boundary=batch_d1befdc3-0a33-4463-8361-5aa28f1c740a

--batch_d1befdc3-0a33-4463-8361-5aa28f1c740a^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
POST /users HTTP/1.1
Accept: application/xml
Content-Length: 689
Content-Type: application/xml
^M
<?xml version="1.0" encoding="UTF-8" standalone="yes"?><user><attributes><entry><key xsi:type="xs:string" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">firstname</key><value xsi:type="xs:string" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">John</value></entry><entry><key xsi:type="xs:string" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">surname</key><value xsi:type="xs:string" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">Doe</value></entry></attributes><id>xxxyyy</id></user>
--batch_d1befdc3-0a33-4463-8361-5aa28f1c740a^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
PUT /users/xxxyyy HTTP/1.1
Accept: application/json
Content-Length: 69
Content-Type: application/json
^M
{"id":"yuyueeee","attributes":{"firstname":"Johnny","surname":"Doe"}}
--batch_d1befdc3-0a33-4463-8361-5aa28f1c740a^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
DELETE /roles/xxxyyy HTTP/1.1
--batch_d1befdc3-0a33-4463-8361-5aa28f1c740a^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
DELETE /users/xxxyyy HTTP/1.1
--batch_d1befdc3-0a33-4463-8361-5aa28f1c740a^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
DELETE /users/yuyueeee HTTP/1.1
--batch_d1befdc3-0a33-4463-8361-5aa28f1c740a--
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
Content-Type: multipart/mixed;boundary=batch_682899de-0bbb-4e7a-8348-fff1d14244c3


--batch_682899de-0bbb-4e7a-8348-fff1d14244c3^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 201 Created
Date: Wed, 25 Jul 2018 10:43:34 GMT
Location: http://localhost:8080/users/xxxyyy
Content-Type: application/xml
^M
<?xml version="1.0" encoding="UTF-8" standalone="yes"?><user><attributes><entry><key xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">firstname</key><value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">John</value></entry><entry><key xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">surname</key><value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema" xsi:type="xs:string">Doe</value></entry></attributes><id>xxxyyy</id></user>
--batch_682899de-0bbb-4e7a-8348-fff1d14244c3^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 200 OK
Date: Wed, 25 Jul 2018 10:43:35 GMT
Content-Type: application/json
^M
{"id":"yuyueeee","attributes":{}}
--batch_682899de-0bbb-4e7a-8348-fff1d14244c3^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 404 Not Found
Content-Length: 0
Date: Wed, 25 Jul 2018 10:43:35 GMT
Allow: DELETE,POST,GET,PUT,OPTIONS,HEAD
^M
--batch_682899de-0bbb-4e7a-8348-fff1d14244c3^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 404 Not Found
Content-Length: 0
Date: Wed, 25 Jul 2018 10:43:35 GMT

--batch_682899de-0bbb-4e7a-8348-fff1d14244c3^M
Content-Type: application/http
Content-Transfer-Encoding: binary
^M
HTTP/1.1 204 No Content
Content-Length: 0
Date: Wed, 25 Jul 2018 10:43:35 GMT

--batch_682899de-0bbb-4e7a-8348-fff1d14244c3--
```
