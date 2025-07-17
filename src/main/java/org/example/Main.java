package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.example.NycFerryAdvancedSimulation.*;

@Command(name = "NycFerryPublisher", mixinStandardHelpOptions = true, version = "1.0",
        description = "Publishes simulated NYC ferry AIS data to an MQTT broker.")
public class Main implements Runnable {

    @Option(names = {"--broker"}, description = "MQTT broker URI (e.g. tcp://localhost:1883)", defaultValue = "tcp://localhost:1883")
    private String broker;

    @Option(names = {"--username"}, description = "Username for MQTT authentication", defaultValue = "admin")
    private String username;

    @Option(names = {"--password"}, description = "Password for MQTT authentication", defaultValue = "admin")
    private String password;

    @Option(names = {"--topic"}, description = "Base topic to publish ferry data", defaultValue = "producers/ferries/data")
    private String topic;

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

        try {
            runSimulation(client, topic);
        } catch (InterruptedException e) {
            System.err.println("Simulation interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    private void runSimulation(Mqtt3AsyncClient client, String topic) throws InterruptedException {
        final int SIMULATION_SECONDS = 24 * 60 * 60;
        System.out.println("Starting infinite simulation...");

        int simSecond = 12 * 60 * 60;  // start at noon
        while (true) {
            int currentSecond = simSecond % SIMULATION_SECONDS;

            for (Ferry ferry : FERRIES) {
                CoordinateAndInfo pos = getFerryPosition(ferry, currentSecond);
                if (pos != null) {
                    String fullTopic = topic + "/" + ferry.mmsi;
                    String message = toCatalogExplorerTrackUpdate(pos);
                    publishMessage(client, fullTopic, message);
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
