package org.example;

import java.util.*;

public class NycFerryAdvancedSimulation {

    static class Coordinate {
        double lon, lat;
        Coordinate(double lon, double lat) { this.lon = lon; this.lat = lat; }
    }

    static class Route {
        String name;
        List<String> stops;
        Route(String name, List<String> stops) { this.name = name; this.stops = stops; }
    }

    static class Ferry {
        String name;
        String mmsi;
        String callSign;
        double draught;
        int dimensionToBow;
        int dimensionToStern;
        int dimensionToPort;
        int dimensionToStarboard;
        Route route;
        List<Integer> startTimesSec;

        Ferry(String name, String mmsi, String callSign,
              double draught, int dimensionToBow, int dimensionToStern, int dimensionToPort, int dimensionToStarboard,
              Route route, List<Integer> startTimesSec) {
            this.name = name;
            this.mmsi = mmsi;
            this.callSign = callSign;
            this.draught = draught;
            this.dimensionToBow = dimensionToBow;
            this.dimensionToStern = dimensionToStern;
            this.dimensionToPort = dimensionToPort;
            this.dimensionToStarboard = dimensionToStarboard;
            this.route = route;
            this.startTimesSec = startTimesSec;
        }
    }

    static final Map<String, Coordinate> STOPS = new HashMap<>();
    static {
        STOPS.put("Staten Island", new Coordinate(-74.07185247730308, 40.64372599586143));
        STOPS.put("Manhattan South", new Coordinate(-74.01183111812227, 40.70094075584476));
        STOPS.put("Waypoint_SI_1", new Coordinate(-74.065, 40.660));
        STOPS.put("Waypoint_SI_2", new Coordinate(-74.030, 40.670));
        STOPS.put("Waypoint_SI_3", new Coordinate(-74.015, 40.685));
        STOPS.put("Wall Street Pier 11", new Coordinate(-74.0055, 40.7032));
        STOPS.put("Brooklyn Army Terminal", new Coordinate(-74.0263199814677, 40.646481073821555));
        STOPS.put("Waypoint_BK_1", new Coordinate(-74.010, 40.685));
        STOPS.put("Waypoint_BK_3", new Coordinate(-74.00990862168231, 40.688724040690865));
        STOPS.put("Waypoint_BK_4", new Coordinate(-74.02479978200442, 40.678026368045664));
        STOPS.put("Waypoint_BK_5", new Coordinate(-74.03164629249736, 40.65440590684508));
        STOPS.put("Astoria Dock", new Coordinate(-73.935718, 40.771726));
        STOPS.put("Roosevelt Island Pier", new Coordinate(-73.949, 40.762));
        STOPS.put("Long Island City Pier", new Coordinate(-73.961, 40.748));
        STOPS.put("East 34th", new Coordinate(-73.97064262198204, 40.743945191164066));
        STOPS.put("Governors Island Dock", new Coordinate(-74.015167, 40.686489));
        STOPS.put("Waypoint_GI_1", new Coordinate(-74.013, 40.690));
    }

    static final Map<String, Integer> TRAVEL_TIMES_SEC = new HashMap<>();
    static {
        TRAVEL_TIMES_SEC.put("Staten Island-Waypoint_SI_1", 9 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_1-Waypoint_SI_2", 7 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_2-Waypoint_SI_3", 5 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_3-Manhattan South", 4 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-Waypoint_SI_3", 4 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_3-Waypoint_SI_2", 5 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_2-Waypoint_SI_1", 7 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_1-Staten Island", 9 * 60);
        TRAVEL_TIMES_SEC.put("Wall Street Pier 11-Waypoint_BK_3", 4 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_3-Waypoint_BK_1", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_1-Waypoint_BK_4", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_4-Waypoint_BK_5", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_5-Brooklyn Army Terminal", 4 * 60);
        TRAVEL_TIMES_SEC.put("Brooklyn Army Terminal-Waypoint_BK_5", 4 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_5-Waypoint_BK_4", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_4-Waypoint_BK_1", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_1-Waypoint_BK_3", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_BK_3-Wall Street Pier 11", 4 * 60);
        TRAVEL_TIMES_SEC.put("Astoria Dock-Roosevelt Island Pier", 7 * 60);
        TRAVEL_TIMES_SEC.put("Roosevelt Island Pier-Long Island City Pier", 6 * 60);
        TRAVEL_TIMES_SEC.put("Long Island City Pier-East 34th", 8 * 60);
        TRAVEL_TIMES_SEC.put("East 34th-Long Island City Pier", 8 * 60);
        TRAVEL_TIMES_SEC.put("Long Island City Pier-Roosevelt Island Pier", 6 * 60);
        TRAVEL_TIMES_SEC.put("Roosevelt Island Pier-Astoria Dock", 7 * 60);
        TRAVEL_TIMES_SEC.put("Governors Island Dock-Waypoint_GI_1", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_GI_1-Manhattan South", 4 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-Waypoint_GI_1", 4 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_GI_1-Governors Island Dock", 3 * 60);
    }

    static final List<Route> ROUTES = List.of(
            new Route("Staten Island - Manhattan", List.of("Staten Island", "Waypoint_SI_1", "Waypoint_SI_2", "Waypoint_SI_3", "Manhattan South")),
            new Route("Wall Street - Brooklyn Army Terminal", List.of("Wall Street Pier 11", "Waypoint_BK_3", "Waypoint_BK_1", "Waypoint_BK_4", "Waypoint_BK_5", "Brooklyn Army Terminal")),
            new Route("Queens - East 34th", List.of("Astoria Dock", "Roosevelt Island Pier", "Long Island City Pier", "East 34th")),
            new Route("Governors Island - Manhattan", List.of("Governors Island Dock", "Waypoint_GI_1", "Manhattan South"))
    );

    static List<Integer> generateTimesSec(int startMin, int intervalMin, int count) {
        List<Integer> times = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            times.add((startMin + i * intervalMin) * 60);
        }
        return times;
    }

    static String aisName(String name) {
        String aisName = name.trim().toUpperCase();
        return aisName.length() > 20 ? aisName.substring(0, 20) : aisName;
    }

    static final List<Ferry> FERRIES = List.of(
            new Ferry(aisName("MV Gov. Alfred E. Smith"), "367587740", "WDI6910",
                    4.2, 12, 8, 7, 7, ROUTES.get(0), generateTimesSec(0, 60, 24)),
            new Ferry(aisName("MV John F. Kennedy"), "367587580", "WDI6901",
                    3.8, 10, 9, 6, 6, ROUTES.get(0), generateTimesSec(30, 60, 24)),
            new Ferry(aisName("MV Sally"), "368710000", "WDI6920",
                    4.0, 11, 8, 7, 7, ROUTES.get(1), generateTimesSec(15, 60, 24)),
            new Ferry(aisName("MV Mischief"), "368710001", "WDI6921",
                    3.9, 10, 9, 6, 6, ROUTES.get(1), generateTimesSec(45, 60, 24)),
            new Ferry(aisName("MV Hallets Point"), "368710002", "WDI6922",
                    4.1, 12, 8, 7, 7, ROUTES.get(2), generateTimesSec(0, 60, 24)),
            new Ferry(aisName("MV Soundview"), "368710003", "WDI6923",
                    3.8, 10, 9, 6, 6, ROUTES.get(2), generateTimesSec(30, 60, 24)),
            new Ferry(aisName("MV Governor"), "367587682", "WDI6909",
                    4.2, 11, 9, 7, 7, ROUTES.get(3), generateTimesSec(0, 40, 36)),
            new Ferry(aisName("MV American Legion"), "367587683", "WDI6911",
                    3.9, 10, 8, 6, 6, ROUTES.get(3), generateTimesSec(20, 40, 36))
    );

    static Coordinate interpolate(Coordinate start, Coordinate end, double t) {
        double lon = start.lon + t * (end.lon - start.lon);
        double lat = start.lat + t * (end.lat - start.lat);
        return new Coordinate(lon, lat);
    }

    static int getTravelTimeSec(String from, String to) {
        return TRAVEL_TIMES_SEC.getOrDefault(from + "-" + to, 0);
    }

    static class CoordinateAndInfo {
        Coordinate coord;
        Ferry ferry;
        String segment;
        String direction;
        int simSecond;
        double heading;
        double speed;

        CoordinateAndInfo(Coordinate coord, Ferry ferry, String segment, String direction, int simSecond,
                          double heading, double speed) {
            this.coord = coord;
            this.ferry = ferry;
            this.segment = segment;
            this.direction = direction;
            this.simSecond = simSecond;
            this.heading = heading;
            this.speed = speed;
        }
    }

    public static CoordinateAndInfo getFerryPosition(Ferry ferry, int simSecond) {
        List<String> stops = ferry.route.stops;

        int forwardTime = 0;
        for (int i = 0; i < stops.size() - 1; i++)
            forwardTime += getTravelTimeSec(stops.get(i), stops.get(i + 1));
        int backwardTime = 0;
        for (int i = stops.size() - 1; i > 0; i--)
            backwardTime += getTravelTimeSec(stops.get(i), stops.get(i - 1));
        int fullTripTime = forwardTime + backwardTime;

        for (int startTime : ferry.startTimesSec) {
            int elapsed = simSecond - startTime;
            if (elapsed < 0 || elapsed > fullTripTime) continue;

            if (elapsed <= forwardTime) {
                int acc = 0;
                for (int i = 0; i < stops.size() - 1; i++) {
                    int segTime = getTravelTimeSec(stops.get(i), stops.get(i + 1));
                    if (elapsed <= acc + segTime) {
                        double t = (elapsed - acc) / (double) segTime;
                        Coordinate from = STOPS.get(stops.get(i));
                        Coordinate to = STOPS.get(stops.get(i + 1));
                        Coordinate pos = interpolate(from, to, t);
                        double heading = calculateHeading(from, to);
                        double speed = calculateDistanceMeters(from, to) / segTime;
                        return new CoordinateAndInfo(pos, ferry,
                                stops.get(i) + "->" + stops.get(i + 1), "forward",
                                simSecond, heading, speed);
                    }
                    acc += segTime;
                }
            } else {
                int backwardElapsed = elapsed - forwardTime;
                int acc = 0;
                for (int i = stops.size() - 1; i > 0; i--) {
                    int segTime = getTravelTimeSec(stops.get(i), stops.get(i - 1));
                    if (backwardElapsed <= acc + segTime) {
                        double t = (backwardElapsed - acc) / (double) segTime;
                        Coordinate from = STOPS.get(stops.get(i));
                        Coordinate to = STOPS.get(stops.get(i - 1));
                        Coordinate pos = interpolate(from, to, t);
                        double heading = calculateHeading(from, to);
                        double speed = calculateDistanceMeters(from, to) / segTime;
                        return new CoordinateAndInfo(pos, ferry,
                                stops.get(i) + "->" + stops.get(i - 1), "backward",
                                simSecond, heading, speed);
                    }
                    acc += segTime;
                }
            }
        }
        return null;
    }

    private static final double EARTH_RADIUS_M = 6371000;

    static double calculateHeading(Coordinate from, Coordinate to) {
        double lat1Rad = Math.toRadians(from.lat);
        double lat2Rad = Math.toRadians(to.lat);
        double deltaLonRad = Math.toRadians(to.lon - from.lon);
        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    static double calculateDistanceMeters(Coordinate from, Coordinate to) {
        double lat1Rad = Math.toRadians(from.lat);
        double lat2Rad = Math.toRadians(to.lat);
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = Math.toRadians(to.lon - from.lon);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static int calculateEtaSeconds(CoordinateAndInfo info) {
        List<String> stops = info.ferry.route.stops;
        if (stops == null) return -1;

        String[] segmentParts = info.segment.split("->");
        if (segmentParts.length != 2) return -1;

        String fromStop = segmentParts[0];
        String toStop = segmentParts[1];

        int totalSegmentTime = getTravelTimeSec(fromStop, toStop);
        if (totalSegmentTime == 0) return -1;

        Coordinate fromCoord = STOPS.get(fromStop);
        Coordinate toCoord = STOPS.get(toStop);
        if (fromCoord == null || toCoord == null) return -1;

        double segmentDistance = calculateDistanceMeters(fromCoord, toCoord);
        double distanceTraveled = calculateDistanceMeters(fromCoord, info.coord);

        double frac = Math.min(1.0, Math.max(0.0, distanceTraveled / segmentDistance));
        int elapsedOnSegment = (int) (frac * totalSegmentTime);

        int remainingOnSegment = totalSegmentTime - elapsedOnSegment;

        int index = stops.indexOf(toStop);
        if (index == -1) return -1;

        int eta = remainingOnSegment;
        if ("forward".equals(info.direction)) {
            for (int i = index; i < stops.size() - 1; i++) {
                eta += getTravelTimeSec(stops.get(i), stops.get(i + 1));
            }
        } else if ("backward".equals(info.direction)) {
            for (int i = index; i > 0; i--) {
                eta += getTravelTimeSec(stops.get(i), stops.get(i - 1));
            }
        } else {
            return -1;
        }
        return eta;
    }

    public static String toCatalogExplorerTrackUpdate(CoordinateAndInfo info) {
        int etaSec = calculateEtaSeconds(info);

        String destination;
        List<String> stops = info.ferry.route.stops;

        if ("forward".equals(info.direction)) {
            destination = stops.get(stops.size() - 1);
        } else if ("backward".equals(info.direction)) {
            destination = stops.get(0);
        } else {
            destination = "";
        }

        return String.format(Locale.US,
                "{" +
                        "\"action\":\"PUT\"," +
                        "\"geometry\":{\"type\":\"Point\",\"coordinates\":[%.6f,%.6f]}," +
                        "\"id\":\"%s\"," +
                        "\"properties\":{" +
                        "\"mmsi\":\"%s\"," +
                        "\"ferry_name\":\"%s\"," +
                        "\"route\":\"%s\"," +
                        "\"segment\":\"%s\"," +
                        "\"direction\":\"%s\"," +
                        "\"destination\":\"%s\"," +
                        "\"timestamp_second\":%d," +
                        "\"heading\":%.1f," +
                        "\"speed_mps\":%.2f," +
                        "\"eta_next_stop_sec\":%d," +
                        "\"draught\":%.1f," +
                        "\"dimensionToBow\":%d," +
                        "\"dimensionToStern\":%d," +
                        "\"dimensionToPort\":%d," +
                        "\"dimensionToStarboard\":%d" +
                        "}" +
                        "}",
                info.coord.lon, info.coord.lat,
                info.ferry.mmsi,
                info.ferry.mmsi,
                info.ferry.name,
                info.ferry.route.name,
                info.segment,
                info.direction,
                destination,
                info.simSecond,
                info.heading,
                info.speed,
                etaSec,
                info.ferry.draught,
                info.ferry.dimensionToBow,
                info.ferry.dimensionToStern,
                info.ferry.dimensionToPort,
                info.ferry.dimensionToStarboard);
    }

}
