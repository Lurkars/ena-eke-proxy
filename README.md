# ENA Exposue Key export proxy

This is a proxy for getting an [Exposure Key Export](https://developers.google.com/android/exposure-notifications/exposure-key-file-format) to an [ESP-ENA](https://github.com/Lurkars/esp-ena) with the *ena-eke-proxy* module. 

### Features implemented

* fetch daily and hourly keys
* return keys as binary for **ESP-ENA ena-eke-proxy**
* upload keys with pre-defined TANs to proxy

### Features planned

* check signature of Exposure Key Export
* upload keys to external server
* signature of binary keydata

### endpoints

### binary payload

| Key Data | Rolling Start Interval Number | Rolling Period | Days Since Onset Of Symptoms |
| :------: | :---------------------------: | :------------: | :--------------------------: |
| 16 bytes |            4 bytes            |    4 bytes     |           4 bytes            |

### daily keys
*/version/v1/diagnosis-keys/country/{countryCode}/date/{dateString}* 
### hourly keys
*/version/v1/diagnosis-keys/country/{countryCode}/date/{dateString}/hour/{hour}* 
### upload keys
*/version/v1/diagnosis-keys* 

### request parameters
*page* and *size* for pagination (default ?page=0&size=500 resulting in first 500 keys)

### responses
* *200 OK* for successfull request with keys in payload
* *204 NO CONTENT* for pagination out of bounds
* *404 NOT FOUND* if keys are not available yet (not fetched)
* *406 NOT ACCEPTABLE* for wrong/not supported country code
* *409 CONFLICT* on error 

## build

mvn clean package -P [*database-profile*]

## configure

Expample application.properties (place in path of ena-eke-proxy.jar)

```
# example with key urls from german App (Corona Warn App)
ena-eke-proxy.daily-url=https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/%s/date/%s
ena-eke-proxy.hourly-url=${ena-eke-proxy.daily-url}/hour/%s
ena-eke-proxy.supported-countries=DE
ena-eke-proxy.source=CWA

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
SyslogIdentifier=enaekeproxy

[Install]
WantedBy=multi-user.target
```