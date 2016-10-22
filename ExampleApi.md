## [GET /api/user]



### URL Parameters
Name        | required | type     | default value | description
--- | ---  | --- | --- | ---
 name | | string | |
    


### Request body

no content

#### Response 200

```json
{
  "id": 1,
  "name": "Jim"
}
```
## [POST /api/user]



### Request body

```json
{
  "id": 2,
  "name": "Hello"
}
```

#### Response 200

```json
{
  "id": 2,
  "name": "Hello"
}
```
## [GET /api/users]



### URL Parameters
Name        | required | type     | default value | description
--- | ---  | --- | --- | ---
 offset | | number | |
     limit | | number | |
    


### Request body

no content

#### Response 200

```json
[{
  "id": 1,
  "name": "John"
}, {
  "id": 2,
  "name": "Jim"
}, {
  "id": 3,
  "name": "Jake"
}, {
  "id": 4,
  "name": "Jacob"
}, {
  "id": 5,
  "name": "Jane"
}]
```
