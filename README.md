# ENA Exposue Key export proxy

This is a proxy for getting an [Exposure Key Export](https://developers.google.com/android/exposure-notifications/exposure-key-file-format) to [esp-ena](https://github.com/Lurkars/esp-ena) with *ena-eke-proxy* module. 

### Features implemented

* fetch daily and hourly keys
* return keys as binary for **ESP-ENA ena-eke-proxy**
* upload keys with pre-defined TANs to proxy

### Features planned

* check signature of Exposure Key Export
* upload keys to external server
* signature of binary keydata

### endpoints

#### binary payload

payload format for a single key both for fetching and upload, concatenation for multiple keys.


| Key Data | Rolling Start Interval Number | Rolling Period | Days Since Onset Of Symptoms |
| :------: | :---------------------------: | :------------: | :--------------------------: |
| 16 bytes |            4 bytes            |    4 bytes     |           4 bytes            |


#### fetching keys

url for daily keys
> */version/v1/diagnosis-keys/country/{countryCode}/date/{dateString}* 

url for hourly keys
> */version/v1/diagnosis-keys/country/{countryCode}/date/{dateString}/hour/{hour}* 

##### request parameters
> *page* and *size* for pagination (default ?page=0&size=500 resulting in first 500 keys)

##### responses
> * *200 OK* for successfull request with keys in payload
> * *204 NO CONTENT* for pagination out of bounds
> * *404 NOT FOUND* if keys are not available yet (not fetched)
> * *406 NOT ACCEPTABLE* for wrong/not supported country code
> * *409 CONFLICT* on error 

#### uploading keys

upload url
> */version/v1/diagnosis-keys* 

##### headers
> *Authorization* with submission token as value

##### responses
> * *200 OK* for successfull upload
> * *400 BAD REQUEST* for errors in payload
> * *401 UNAUTHORIZED* for invalid/missing token in Authorization header

## build

mvn clean package -P [*database-profile*]

available database-profiles, Spring Data JPA is used, see documentation for configuration
* *db-inmemory* in-memory, for testing
* *db-mariadb* MariaDB
* *db-mysql* MySql
* *db-postgresql* ProstgeSQL

## configure

Expample application.properties (place in path of ena-eke-proxy.jar)

```
# example with key urls from german App (Corona Warn App)
ena-eke-proxy.daily-url=https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/%s/date/%s
ena-eke-proxy.hourly-url=${ena-eke-proxy.daily-url}/hour/%s
ena-eke-proxy.supported-countries=DE
ena-eke-proxy.source=CWA
ena-eke-proxy.debug.submission-keys=AAAAAAAAAA,BBBBBBBBBB,CCCCCCCCCC,ZZZZZZZZZZ,1234567890

# example with mariadb on localhost
spring.datasource.url=jdbc:mariadb://localhost:3306/databasename
spring.datasource.username=databaseuser
spring.datasource.password=databasepassword
spring.jpa.hibernate.ddl-auto=update
```

## run

simple run *java -jar ena-eke-proxy.jar*

alternative systemd service example:

```
[Unit]
Description=ena eke proxy
After=syslog.target
Restart=on-failure

[Service]
User=enaekeproxy
ExecStart=/var/enaekeproxy/ena-eke-proxy.jar.jar
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=ena-eke-proxy

[Install]
WantedBy=multi-user.target
```
