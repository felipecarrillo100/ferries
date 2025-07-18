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
- MQTT Broker (e.g. Mosquitto, HiveMQ)

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
| `-f`, `--format`      | The format to send the tracks catex or ais|catex                             |

---

## 📋 Example CLI Help Output

```bash
$ java -jar ferry-sim.jar --help

Usage: NycFerryPublisher [OPTIONS]
Publishes simulated NYC ferry AIS data to an MQTT broker.

Options:
  -b, --broker     MQTT broker URI (e.g. tcp://localhost:1883)
                   Default: tcp://localhost:1883
  -u, --username   Username for MQTT authentication
                   Default: admin
  -p, --password   Password for MQTT authentication
                   Default: admin
  -t, --topic      Base topic to publish ferry data
                   Default: producers/ferries/data
  -h, --help       Show this help message and exit.
  -V, --version    Print version information and exit.
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
    "coordinates": [-74.010, 40.685]
  },
  "id": "FerryC",
  "properties": {
    "ferry_name": "FerryC",
    "route": "Wall Street - Brooklyn Army Terminal",
    "segment": "Waypoint_BK_3->Waypoint_BK_1",
    "direction": "forward",
    "timestamp_second": 43200,
    "time": "12:00:00"
  }
}
```

This is compliant with `Catalog Explorer` Live Tracks expectations.

---

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
