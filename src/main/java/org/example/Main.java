package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import io.github.felipecarrillo100.ais.AisPositionMessage;
import io.github.felipecarrillo100.ais.AisStaticMessage;
import io.github.felipecarrillo100.ais.AisEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.example.NycFerryAdvancedSimulation.*;

@Command(name = "NycFerryPublisher", mixinStandardHelpOptions = true, version = "1.0",
        description = "Publishes simulated NYC ferry AIS or Catalog Explorer data to an MQTT broker.")
public class Main implements Runnable {

    @Option(names = {"--broker", "-b"}, description = "MQTT broker URI (e.g. tcp://localhost:1883)", defaultValue = "tcp://localhost:1883")
    private String broker;

    @Option(names = {"--username", "-u"}, description = "Username for MQTT authentication", defaultValue = "admin")
    private String username;

    @Option(names = {"--password", "-p"}, description = "Password for MQTT authentication", defaultValue = "admin")
    private String password;

    @Option(names = {"--topic", "-t"}, description = "Base topic to publish ferry data", defaultValue = "producers/ferries/data")
    private String topic;

    @Option(names = {"--format", "-f"}, description = "Message format: catex (default) or ais", defaultValue = "catex")
    private String format;

    private static final int STATIC_MSG_BASE_INTERVAL = 60;
    private static final int STATIC_MSG_RANDOM_OFFSET = 10;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // Parse broker
        String hostPort = broker.replace("tcp://", "").replace("ssl://", "");
        String[] split = hostPort.split(":");
        String host = split[0];
        int port = (split.length > 1) ? Integer.parseInt(split[1])
                : (broker.startsWith("ssl://") ? 8883 : 1883);
        boolean useTls = broker.startsWith("ssl://");

        var builder = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(host)
                .serverPort(port);

        if (useTls) {
            builder = builder.sslWithDefaultConfig();
        }

        Mqtt3AsyncClient client = builder.buildAsync();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown detected. Disconnecting...");
            client.disconnect()
                    .whenComplete((ack, ex) -> System.out.println("Disconnected. Bye!"));
        }));

        var connectBuilder = client.connectWith();

        if (!username.isEmpty()) {
            connectBuilder.simpleAuth()
                    .username(username)
                    .password(password.getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth();
        }

        try {
            Mqtt3ConnAck connAck = connectBuilder.send().join();
            System.out.println("Connected to broker: " + broker);
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            System.exit(1);
        }

        // State for static message sending per ferry: count of positions sent since last static message
        Map<String, Integer> positionCountSinceStatic = new HashMap<>();
        // Static message threshold per ferry (60 ± random 0-10)
        Map<String, Integer> staticThresholds = new HashMap<>();

        // Initialize maps for each ferry
        for (Ferry ferry : FERRIES) {
            positionCountSinceStatic.put(ferry.mmsi, 0);
            staticThresholds.put(ferry.mmsi, STATIC_MSG_BASE_INTERVAL + ThreadLocalRandom.current().nextInt(-STATIC_MSG_RANDOM_OFFSET, STATIC_MSG_RANDOM_OFFSET + 1));
        }

        try {
            runSimulation(client, topic, positionCountSinceStatic, staticThresholds);
        } catch (InterruptedException e) {
            System.err.println("Simulation interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    private void runSimulation(Mqtt3AsyncClient client, String topic,
                               Map<String, Integer> positionCountSinceStatic,
                               Map<String, Integer> staticThresholds) throws InterruptedException {
        final int SIMULATION_SECONDS = 24 * 60 * 60;
        System.out.println("Starting infinite simulation with format: " + format);

        int simSecond = 12 * 60 * 60;  // start at noon

        while (true) {
            int currentSecond = simSecond % SIMULATION_SECONDS;

            for (Ferry ferry : FERRIES) {
                CoordinateAndInfo pos = getFerryPosition(ferry, currentSecond);
                if (pos != null) {
                    String fullTopic = topic + "/" + ferry.mmsi;

                    if ("ais".equalsIgnoreCase(format)) {
                        // Send position AIS messages
                        AisPositionMessage position = new AisPositionMessage();
                        position.setMmsi(Integer.parseInt(pos.mmsi));
                        position.setLat(pos.coord.lat);
                        position.setLon(pos.coord.lon);
                        position.setTimestamp(currentSecond % 60);

                        // Set speed over ground (SOG) in knots: convert m/s to knots (1 m/s ≈ 1.94384 knots)
                        double sogKnots = pos.speed * 1.94384;
                        position.setSog(sogKnots);

                        // Set course over ground (COG) in degrees (0-360)
                        position.setCog(pos.heading);     // course over ground
                        position.setHeading((int)Math.round(pos.heading));  // round float heading to int degrees

                        position.setNavStatus(0);

                        List<String> positionSentences = AisEncoder.encodePositionMessage(position);
                        for (String sentence : positionSentences) {
                            publishMessage(client, fullTopic, sentence);
                        }

                        // Update counter for static messages
                        int count = positionCountSinceStatic.get(ferry.mmsi) + 1;
                        positionCountSinceStatic.put(ferry.mmsi, count);

                        int threshold = staticThresholds.get(ferry.mmsi);
                        if (count >= threshold) {
                            // Send static AIS messages
                            AisStaticMessage staticMsg = new AisStaticMessage();
                            staticMsg.setMmsi(Integer.parseInt(ferry.mmsi));
                            staticMsg.setName(ferry.name);
                            staticMsg.setCallsign("");
                            staticMsg.setShipType(70);
                            staticMsg.setDimensionToBow(0);
                            staticMsg.setDimensionToStern(0);
                            staticMsg.setDimensionToPort(0);
                            staticMsg.setDimensionToStarboard(0);

                            List<String> staticSentences = AisEncoder.encodeStaticMessage(staticMsg);
                            for (String sentence : staticSentences) {
                                publishMessage(client, fullTopic, sentence);
                            }

                            // Reset counter and set new randomized threshold
                            positionCountSinceStatic.put(ferry.mmsi, 0);
                            staticThresholds.put(ferry.mmsi, STATIC_MSG_BASE_INTERVAL + ThreadLocalRandom.current().nextInt(-STATIC_MSG_RANDOM_OFFSET, STATIC_MSG_RANDOM_OFFSET + 1));
                        }
                    } else {
                        // Default to catex format
                        String message = toCatalogExplorerTrackUpdate(pos);
                        publishMessage(client, fullTopic, message);
                    }
                }
            }

            TimeUnit.MILLISECONDS.sleep(1000);
            simSecond++;
        }
    }

    private void publishMessage(Mqtt3AsyncClient client, String topic, String payload) {
        client.publishWith()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("Publish failed: " + ex.getMessage());
                    } else {
                        System.out.println("Published: " + payload);
                    }
                });
    }
}
