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
        // Parse broker info
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

        Map<String, Integer> positionCountSinceStatic = new HashMap<>();
        Map<String, Integer> staticThresholds = new HashMap<>();

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
                NycFerryAdvancedSimulation.CoordinateAndInfo pos = getFerryPosition(ferry, currentSecond);
                if (pos != null) {
                    String fullTopic = topic + "/" + ferry.mmsi;

                    if ("ais".equalsIgnoreCase(format)) {
                        AisPositionMessage position = new AisPositionMessage();
                        position.setMmsi(Integer.parseInt(pos.ferry.mmsi));
                        position.setLat(pos.coord.lat);
                        position.setLon(pos.coord.lon);
                        position.setTimestamp(currentSecond % 60);

                        double sogKnots = pos.speed * 1.94384;
                        position.setSog(sogKnots);

                        position.setCog(pos.heading);
                        position.setHeading((int) Math.round(pos.heading));
                        position.setNavStatus(0);

                        List<String> positionSentences = AisEncoder.encodePositionMessage(position);
                        for (String sentence : positionSentences) {
                            publishMessage(client, fullTopic, sentence);
                        }

                        int count = positionCountSinceStatic.get(ferry.mmsi) + 1;
                        positionCountSinceStatic.put(ferry.mmsi, count);

                        int threshold = staticThresholds.get(ferry.mmsi);
                        if (count >= threshold) {
                            AisStaticMessage staticMsg = new AisStaticMessage();
                            staticMsg.setMmsi(Integer.parseInt(ferry.mmsi));
                            staticMsg.setName(ferry.name);
                            staticMsg.setCallsign(ferry.callSign);
                            staticMsg.setShipType(60);   // Passenger ship
                            staticMsg.setDimensionToBow(ferry.dimensionToBow);
                            staticMsg.setDimensionToStern(ferry.dimensionToStern);
                            staticMsg.setDimensionToPort(ferry.dimensionToPort);
                            staticMsg.setDimensionToStarboard(ferry.dimensionToStarboard);
                            staticMsg.setDraught(ferry.draught);

                            // Add ETA goes here
// Calculate ETA seconds (time remaining until ferry arrival at next stop)
                            int etaSeconds = NycFerryAdvancedSimulation.calculateEtaSeconds(pos);

                            if (etaSeconds < 0) {
                                // No valid ETA, mark as unavailable per AIS spec (24 = hour unavailable, 60 = minute unavailable)
                                staticMsg.setEtaMonth(0);
                                staticMsg.setEtaDay(0);
                                staticMsg.setEtaHour(24);
                                staticMsg.setEtaMinute(60);
                            } else {
                                // Compute ETA fields assuming simulation day = today starting at midnight
                                java.time.LocalDateTime now = java.time.LocalDateTime.now()
                                        .truncatedTo(java.time.temporal.ChronoUnit.DAYS)
                                        .plusSeconds(simSecond);

                                java.time.LocalDateTime etaDateTime = now.plusSeconds(etaSeconds);

                                staticMsg.setEtaMonth(etaDateTime.getMonthValue());
                                staticMsg.setEtaDay(etaDateTime.getDayOfMonth());
                                staticMsg.setEtaHour(etaDateTime.getHour());
                                staticMsg.setEtaMinute(etaDateTime.getMinute());
                            }

                            String destination = getDestinationName(pos);
                            staticMsg.setDestination(destination);

                            List<String> staticSentences = AisEncoder.encodeStaticMessage(staticMsg);
                            for (String sentence : staticSentences) {
                                publishMessage(client, fullTopic, sentence);
                            }

                            positionCountSinceStatic.put(ferry.mmsi, 0);
                            staticThresholds.put(ferry.mmsi, STATIC_MSG_BASE_INTERVAL + ThreadLocalRandom.current().nextInt(-STATIC_MSG_RANDOM_OFFSET, STATIC_MSG_RANDOM_OFFSET + 1));
                        }
                    } else {
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
