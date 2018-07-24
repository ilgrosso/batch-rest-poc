# batch-rest-poc
PoC showing how to implement batch processing via REST with Apache CXF

Inspired by [OData 4.0's Batch requests](http://docs.oasis-open.org/odata/odata/v4.0/os/part1-protocol/odata-v4.0-os-part1-protocol.html#_Toc372793748), this PoC shows how to handle a batch request composed by a sequence of REST requests.

### Sample request
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
