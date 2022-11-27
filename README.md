CSV Server
==========
This is a simple server for serving CSV data from CSV files partitioned by day in order to visualize them using the [Grafana CSV Plugin](https://grafana.com/grafana/plugins/marcusolsson-csv-datasource/).

Data structure
--------------
Data is organised using paths like this:
```
sensors/${room}/${type}/${year}/${month}-${day}.csv
```
Month and day always have two digits and must be in timezone UTC.

The CSV files should have one heading line, which is ignored by the CSV-Server.

Each line in the the CSV files should begin with a timestamp which can be parsed by Java's [Instant.parse()](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html#parse-java.lang.CharSequence-) (e.g. `2007-12-03T10:15:30.00Z`) followed by a comma and then arbitrary data.


Requesting CSV Data
-------------------
Files may be requested via HTTP for example using this GET-Request:
```
http://localhost:3001/${type}?from=${unix_timestamp_from}&to=${unix_timestamp_to}
```
Timestamps should be a number representing milliseconds. With the Grafana CSV-Plugin this can be achieved by setting in the "Params" tab the following:

| Key  | Value   |
|------|---------|
| from | $__from |
| to   | $__to   |


Processing of CSV data
----------------------
The CSV Server will first search all rooms for the requested measurement data. If no datapoint is found that exactly matches the query parameter `from`, then it begins with the first data point before `from` (if such exists) so that the resulting graph can always begin at the edge of requested data.
Similarly if no data point matches exactly `to`, then the last point which is selected is the first point greater than `to`.

The results from all measurements is merged into a single CSV file using "outer join". Thus, all rooms with the same measurement type can be displayed in a single graph. Due to the outer join many `null` values will be in the resulting stream. Set `Connect null values` to `Always` in Grafana time series for continues graphs.


Running CSV Server
------------------
Run `./gradlew jar` to create a Jar file. Run using `java -jar /path/to/csv-server.jar`. Working direcrtory must be a directory which contains the directory `sensors`.


Motivation
----------
Probably the "better" way is to use a [time series database](https://en.wikipedia.org/wiki/Time_series_database).

However, in some cases working with simple CSV files is just simpler, although less powerful. For such a case this software provides a simple connection to Grafana.
