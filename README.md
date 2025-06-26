# 🛳️ NYC Ferry Advanced Simulation with MQTT Publisher

This Java application simulates real-world NYC ferry routes and publishes live ferry positions as MQTT messages. It includes realistic travel times, geographic waypoints over water, and outputs in a structured GeoJSON-like format—perfect for map visualizations or system integration.

---

## 📦 Project Structure

- `NycFerryAdvancedSimulation.java` — Contains route definitions, ferry schedules, travel logic, and message formatting.
- `Main.java` — Connects to an MQTT broker and continuously publishes ferry positions based on simulation time.

---

## 🚀 Features

- ⛴️ Realistic simulation of NYC ferry routes
- 🧭 Waypoint-based routing to avoid land paths
- 🕒 24-hour round-trip scheduling
- 📡 MQTT publishing in GeoJSON-style format
- 🔄 Continuous live simulation with per-second updates

---

## 🧰 Requirements

- Java 17+
- Maven 3.6+
- MQTT Broker (e.g. Mosquitto, HiveMQ)

---

## 📦 Maven Setup

This project uses Maven. The dependency to use MQTT is:

```xml
<dependency>
  <groupId>com.hivemq</groupId>
  <artifactId>hivemq-mqtt-client</artifactId>
  <version>1.3.7</version>
</dependency>
```

---

## ▶️ How to Run

### Build the project

```bash
mvn clean compile
```

### Run the simulation

```bash
mvn exec:java
```

Or with custom arguments:

```bash
mvn exec:java -Dexec.args="tcp://localhost:1883 admin admin producers/ferries/data"
```

**Arguments:**
1. MQTT broker URL (default: `tcp://localhost:1883`)
2. MQTT username (default: `admin`)
3. MQTT password (default: `admin`)
4. Base topic (default: `producers/ferries/data`)

---

## ⚠️ Catalog Explorer MQTT/STOMP Compatibility

Catalog Explorer uses **STOMP** protocol and does **not** understand MQTT natively.  
Instead, it relies on the message broker to translate between MQTT and STOMP protocols.

If you are using **ActiveMQ** as your broker, simply enable both **STOMP** and **MQTT** protocols.  
ActiveMQ will automatically and transparently translate MQTT topics to STOMP destinations.

**Note:** ActiveMQ translates the MQTT topic

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

This is compliant with `Catalog Explorer` Live Tracks messaging that expects:

```json
{
  "action": "...",           // ADD, PUT, DELETE, PATCH
  "geometry": {...},         // GeoJSON Point
  "id": "...",               // Unique vehicle ID
  "properties": {...}        // Any app-specific metadata
}
```

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
- Loosely based on real-world NYC ferry routes and pier coordinates
