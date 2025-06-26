package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.example.NycFerryAdvancedSimulation.*;

public class Main {

    private static final String DEFAULT_BROKER = "tcp://localhost:1883";
    private static final String DEFAULT_TOPIC = "producers/ferries/data";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    public static void main(String[] args) throws InterruptedException {

        // Read CLI args or use defaults
        String broker = args.length > 0 ? args[0] : DEFAULT_BROKER;
        String username = args.length > 1 ? args[1] : DEFAULT_USERNAME;
        String password = args.length > 2 ? args[2] : DEFAULT_PASSWORD;
        String topic   = args.length > 3 ? args[3] : DEFAULT_TOPIC;

        // Parse broker URL
        String hostPort = broker.replace("tcp://", "").replace("ssl://", "");
        String[] split = hostPort.split(":");
        String host = split[0];
        int port = (split.length > 1) ? Integer.parseInt(split[1])
                : (broker.startsWith("ssl://") ? 8883 : 1883);
        boolean useTls = broker.startsWith("ssl://");

        // Build MQTT client
        var builder = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(host)
                .serverPort(port);

        if (useTls) {
            builder = builder.sslWithDefaultConfig();
        }

        Mqtt3AsyncClient client = builder.buildAsync();

        // Graceful shutdown on CTRL+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown detected. Disconnecting...");
            client.disconnect()
                    .whenComplete((ack, ex) -> System.out.println("Disconnected. Bye!"));
        }));

        // Connect to broker
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

        // Start publishing loop
        runSimulation(client, topic);
    }

    private static void runSimulationTest(Mqtt3AsyncClient client, String topic) throws InterruptedException {
        int counter = 0;
        while (true) {
            String payload = "Message number " + counter++;
            publishMessage(client, topic, payload);

            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static void runSimulation(Mqtt3AsyncClient client, String topic) throws InterruptedException {
            final int SIMULATION_SECONDS = 24 * 60 * 60;
            System.out.println("Starting infinite simulation...");

            int simSecond = 12 * 60 * 60;  // We start simulation at noon
            while (true) {
                int currentSecond = simSecond % SIMULATION_SECONDS;

                for (Ferry ferry : FERRIES) {
                    CoordinateAndInfo pos = getFerryPosition(ferry, currentSecond);
                    if (pos != null) {
                        String fullTopic = topic + "/" + ferry.name;
                        String message = toCatalogExplorerTrackUpdate(pos);
                        publishMessage(client, fullTopic, message);
                    }
                }

                //TimeUnit.SECONDS.sleep(1);
                TimeUnit.MILLISECONDS.sleep(1000);
                simSecond++;
            }
    }


    private static void publishMessage(Mqtt3AsyncClient client, String topic, String payload) {
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
