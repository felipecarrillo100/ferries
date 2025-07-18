# 🛳️ NYC Ferry Advanced Simulation with MQTT Publisher

This Java application simulates real-world NYC ferry routes and publishes live ferry positions as MQTT messages. It includes realistic travel times, geographic waypoints over water, and outputs in a structured GeoJSON-like format—perfect for map visualizations or system integration.

---

## 📦 Project Structure

- `NycFerryAdvancedSimulation.java` — Contains route definitions, ferry schedules, travel logic, and message formatting.
- `Main.java` — Connects to an MQTT broker and continuously publishes ferry positions based on simulation time. Now includes a robust command-line interface using [Picocli](https://picocli.info/).

---

## 🚀 Features

- ⛴️ Realistic simulation of NYC ferry routes
- 🧭 Waypoint-based routing to avoid land paths
- 🕒 24-hour round-trip scheduling
- 📡 MQTT publishing in GeoJSON-style format
- 🔄 Continuous live simulation with per-second updates
- 🧾 Flexible CLI with `--broker`, `--username`, `--topic`, etc.

---

## 🧰 Requirements

- Java 17+
- Maven 3.6+
- ActiveMQ

---

## 📦 Maven Setup

This project uses Maven. Required dependencies:

```xml
<!-- MQTT client -->
<dependency>
  <groupId>com.hivemq</groupId>
  <artifactId>hivemq-mqtt-client</artifactId>
  <version>1.3.7</version>
</dependency>

<!-- Picocli CLI parser -->
<dependency>
  <groupId>info.picocli</groupId>
  <artifactId>picocli</artifactId>
  <version>4.7.5</version>
</dependency>

<!-- Your AIS library from GitHub via JitPack -->
<dependency>
<groupId>io.github.felipecarrillo100</groupId>
<artifactId>ais-nmea-encoder-decoder</artifactId>
<version>1.0.2</version>
</dependency>
```

---

## ▶️ How to Run

### Build the project

```bash
mvn clean compile
```

### Run the simulation with defaults

```bash
mvn exec:java
```

### Run the simulation with custom options

```bash
mvn exec:java -Dexec.args="--broker tcp://localhost:1883 --username myuser --password mypass --topic ferries/data"
```

---

## 🛠️ Available CLI Options

| Option (Short / Long) | Description                               | Default                  |
|-----------------------|-------------------------------------------|--------------------------|
| `-b`, `--broker`      | MQTT broker URI                           | `tcp://localhost:1883`   |
| `-u`, `--username`    | MQTT username                             | `admin`                  |
| `-p`, `--password`    | MQTT password                             | `admin`                  |
| `-t`, `--topic`       | Base topic to publish ferry data          | `producers/ferries/data` |
| `-h`, `--help`        | Show usage help                           |                          |
| `-V`, `--version`     | Show version info                         |                          |
| `-f`, `--format`      | Output format: `catex` or `ais` |`catex`                             |

---

## 📋 Example CLI Help Output

```bash
$ java -jar ferry-sim.jar  --help
Usage: NycFerryPublisher [-hV] [-b=<broker>] [-f=<format>] [-p=<password>]
                         [-t=<topic>] [-u=<username>]
Publishes simulated NYC ferry AIS or Catalog Explorer data to an MQTT broker.
  -b, --broker=<broker>   MQTT broker URI (e.g. tcp://localhost:1883)
  -f, --format=<format>   Message format: catex (default) or ais
  -h, --help              Show this help message and exit.
  -p, --password=<password>
                          Password for MQTT authentication
  -t, --topic=<topic>     Base topic to publish ferry data
  -u, --username=<username>
                          Username for MQTT authentication
  -V, --version           Print version information and exit.
```

---

## ⚠️ Catalog Explorer MQTT/STOMP Compatibility

Catalog Explorer uses **STOMP**, not MQTT directly. To bridge the two protocols, use a broker like **ActiveMQ** that supports both MQTT and STOMP.

#### Example Topic Mapping:
- MQTT topic: `producers/ferries/data`
- STOMP topic: `/topic/producers.ferries.data`

This allows MQTT producers and STOMP consumers to communicate transparently.

---

## 🛰️ MQTT Message Example

Each ferry sends a message in the Catalog Explorer Live Tracks format:

```json
{
  "action": "PUT",
  "geometry": {
    "type": "Point",
    "coordinates": [
      -74.028961,
      40.650410
    ]
  },
  "id": "368710001",
  "properties": {
    "mmsi": "368710001",
    "ferry_name": "MV MISCHIEF",
    "route": "Wall Street - Brooklyn Army Terminal",
    "segment": "Waypoint_BK_5->Brooklyn Army Terminal",
    "direction": "forward",
    "destination": "BROOKLYN ARMY TERMIN",
    "timestamp_second": 43201,
    "heading": 153.0,
    "speed_mps": 4.12,
    "eta_next_stop_sec": 120,
    "draught": 3.9,
    "dimensionToBow": 10,
    "dimensionToStern": 9,
    "dimensionToPort": 6,
    "dimensionToStarboard": 6
  }
}
```

This is compliant with `Catalog Explorer` Live Tracks default format.

---

## 🛰️ AIS Message Example

Optionally, this sample  can also send NMEA AIS messages (Options `-f ais`):

```CSV
!AIVDM,1,1,7,A,15O`AL@P1@Je7lvG@bIUvDj20000,0*47
!AIVDM,1,1,9,A,15O`ALPP18JeS8dGE6cHlo420000,0*0A
!AIVDM,1,1,7,A,15NSiHPP0fJe;onGAw0@vPj20000,0*15
!AIVDM,1,1,9,A,15O`ALhP1TJeLEHGDDciBA220000,0*44
```
Position updates are send every second, while static data (such as vessel name) are send every minute

## 🗺️ Included Routes

- Staten Island ↔ Manhattan South
- Wall Street ↔ Brooklyn Army Terminal
- Astoria ↔ Roosevelt Island ↔ Long Island City ↔ East 34th Street
- Governors Island ↔ Manhattan South

---

## 📄 License

MIT License

---

## 🙌 Acknowledgments

- Built using the [HiveMQ MQTT Client](https://github.com/hivemq/hivemq-mqtt-client)
- Command-line interface powered by [Picocli](https://picocli.info/)
- Loosely based on real-world NYC ferry routes and pier coordinates
