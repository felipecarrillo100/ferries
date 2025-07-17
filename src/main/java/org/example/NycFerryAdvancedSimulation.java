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
        String mmsi;    // MMSI number as string (or int if you prefer)

        Route route;
        List<Integer> startTimesSec;
        Ferry(String name,  String mmsi, Route route, List<Integer> startTimesSec) {
            this.name = name;
            this.mmsi = mmsi;
            this.route = route;
            this.startTimesSec = startTimesSec;
        }
    }

    static final Map<String, Coordinate> STOPS = new HashMap<>();
    static {
        // Staten Island - Manhattan
        STOPS.put("Staten Island", new Coordinate(-74.07185247730308, 40.64372599586143));
        STOPS.put("Manhattan South", new Coordinate(-74.01183111812227, 40.70094075584476));

        STOPS.put("Waypoint_SI_1", new Coordinate(-74.065, 40.660));
        STOPS.put("Waypoint_SI_2", new Coordinate(-74.030, 40.670));
        STOPS.put("Waypoint_SI_3", new Coordinate(-74.015, 40.685));

        // Wall Street - Brooklyn Army Terminal (Realistic)
        STOPS.put("Wall Street Pier 11", new Coordinate(-74.0055, 40.7032));
        STOPS.put("Brooklyn Army Terminal", new Coordinate(-74.0263199814677, 40.646481073821555 ));

        STOPS.put("Waypoint_BK_1", new Coordinate(-74.010, 40.685));
        STOPS.put("Waypoint_BK_3", new Coordinate(-74.00990862168231, 40.688724040690865));
        STOPS.put("Waypoint_BK_4", new Coordinate(-74.02479978200442, 40.678026368045664));
        STOPS.put("Waypoint_BK_5", new Coordinate(-74.03164629249736, 40.65440590684508));

        // Queens - East 34th
        STOPS.put("Astoria Dock", new Coordinate(-73.935718, 40.771726));
        STOPS.put("Roosevelt Island Pier", new Coordinate(-73.949, 40.762));
        STOPS.put("Long Island City Pier", new Coordinate(-73.961, 40.748));
        STOPS.put("East 34th", new Coordinate(-73.97064262198204, 40.743945191164066 ));

        // Governors Island - Manhattan
        STOPS.put("Governors Island Dock", new Coordinate(-74.015167, 40.686489));
        STOPS.put("Waypoint_GI_1", new Coordinate(-74.013, 40.690));
    }

    static final Map<String, Integer> TRAVEL_TIMES_SEC = new HashMap<>();
    static {
        // Staten Island
        TRAVEL_TIMES_SEC.put("Staten Island-Waypoint_SI_1", 9 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_1-Waypoint_SI_2", 7 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_2-Waypoint_SI_3", 5 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_3-Manhattan South", 4 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-Waypoint_SI_3", 4 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_3-Waypoint_SI_2", 5 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_2-Waypoint_SI_1", 7 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_SI_1-Staten Island", 9 * 60);

        // Wall Street - Brooklyn Army Terminal (with water waypoints)
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

        // Queens
        TRAVEL_TIMES_SEC.put("Astoria Dock-Roosevelt Island Pier", 7 * 60);
        TRAVEL_TIMES_SEC.put("Roosevelt Island Pier-Long Island City Pier", 6 * 60);
        TRAVEL_TIMES_SEC.put("Long Island City Pier-East 34th", 8 * 60);
        TRAVEL_TIMES_SEC.put("East 34th-Long Island City Pier", 8 * 60);
        TRAVEL_TIMES_SEC.put("Long Island City Pier-Roosevelt Island Pier", 6 * 60);
        TRAVEL_TIMES_SEC.put("Roosevelt Island Pier-Astoria Dock", 7 * 60);

        // Governors Island
        TRAVEL_TIMES_SEC.put("Governors Island Dock-Waypoint_GI_1", 3 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_GI_1-Manhattan South", 4 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-Waypoint_GI_1", 4 * 60);
        TRAVEL_TIMES_SEC.put("Waypoint_GI_1-Governors Island Dock", 3 * 60);
    }

    static final List<Route> ROUTES = List.of(
            new Route("Staten Island - Manhattan",
                    List.of("Staten Island", "Waypoint_SI_1", "Waypoint_SI_2", "Waypoint_SI_3", "Manhattan South")),
            new Route("Wall Street - Brooklyn Army Terminal",
                    List.of("Wall Street Pier 11", "Waypoint_BK_3", "Waypoint_BK_1", "Waypoint_BK_4", "Waypoint_BK_5", "Brooklyn Army Terminal")),
            new Route("Queens - East 34th",
                    List.of("Astoria Dock", "Roosevelt Island Pier", "Long Island City Pier", "East 34th")),
            new Route("Governors Island - Manhattan",
                    List.of("Governors Island Dock", "Waypoint_GI_1", "Manhattan South"))
    );

    static List<Integer> generateTimesSec(int startMin, int intervalMin, int count) {
        List<Integer> times = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            times.add((startMin + i * intervalMin) * 60);
        }
        return times;
    }

    static final List<Ferry> FERRIES = List.of(
            // Staten Island Ferry (sample vessels)
            new Ferry("MV Gov. Alfred E. Smith", "367587740", ROUTES.get(0), generateTimesSec(0, 60, 24)),
            new Ferry("MV John F. Kennedy", "367587580", ROUTES.get(0), generateTimesSec(30, 60, 24)),

            // East River Ferry / NYC Ferry vessels
            new Ferry("MV Sally", "368710000", ROUTES.get(1), generateTimesSec(15, 60, 24)),
            new Ferry("MV Mischief", "368710001", ROUTES.get(1), generateTimesSec(45, 60, 24)),

            // Astoria / East 34th / LIC route ferries
            new Ferry("MV Hallets Point", "368710002", ROUTES.get(2), generateTimesSec(0, 60, 24)),
            new Ferry("MV Soundview", "368710003", ROUTES.get(2), generateTimesSec(30, 60, 24)),

            // Governors Island / Manhattan South route
            new Ferry("MV Governor", "367587682", ROUTES.get(3), generateTimesSec(0, 40, 36)),
            new Ferry("MV American Legion", "367587683", ROUTES.get(3), generateTimesSec(20, 40, 36))
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
        String ferryName, mmsi, routeName, segment, direction;
        int simSecond;
        CoordinateAndInfo(Coordinate c, String ferryName, String mmsi, String routeName,
                          String segment, String direction, int simSecond) {
            this.coord = c; this.ferryName = ferryName; this.mmsi = mmsi; this.routeName = routeName;
            this.segment = segment; this.direction = direction; this.simSecond = simSecond;
        }
    }

    public static CoordinateAndInfo getFerryPosition(Ferry ferry, int simSecond) {
        List<String> stops = ferry.route.stops;
        int forwardTime = 0;
        for (int i = 0; i < stops.size() - 1; i++) {
            forwardTime += getTravelTimeSec(stops.get(i), stops.get(i + 1));
        }
        int backwardTime = 0;
        for (int i = stops.size() - 1; i > 0; i--) {
            backwardTime += getTravelTimeSec(stops.get(i), stops.get(i - 1));
        }
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
                        Coordinate coord = interpolate(STOPS.get(stops.get(i)), STOPS.get(stops.get(i + 1)), t);
                        return new CoordinateAndInfo(coord, ferry.name, ferry.mmsi, ferry.route.name,
                                stops.get(i) + "->" + stops.get(i + 1), "forward", simSecond);
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
                        Coordinate coord = interpolate(STOPS.get(stops.get(i)), STOPS.get(stops.get(i - 1)), t);
                        return new CoordinateAndInfo(coord, ferry.name, ferry.mmsi, ferry.route.name,
                                stops.get(i) + "->" + stops.get(i - 1), "backward", simSecond);
                    }
                    acc += segTime;
                }
            }
        }
        return null;
    }

    public static String toCatalogExplorerTrackUpdate(CoordinateAndInfo info) {
        int h = info.simSecond / 3600;
        int m = (info.simSecond % 3600) / 60;
        int s = info.simSecond % 60;
        return "{\n" +
                "\"action\": \"PUT\",\n" +
                "\"geometry\": {\n" +
                "  \"type\": \"Point\",\n" +
                "  \"coordinates\": [" + info.coord.lon + ", " + info.coord.lat + "]\n" +
                "},\n" +
                "\"id\": \"" + info.ferryName + "\",\n" +
                "\"properties\": {\n" +
                "  \"ferry_name\": \"" + info.ferryName + "\",\n" +
                "  \"mmsi\": \"" + info.mmsi + "\",\n" +
                "  \"route\": \"" + info.routeName + "\",\n" +
                "  \"segment\": \"" + info.segment + "\",\n" +
                "  \"direction\": \"" + info.direction + "\",\n" +
                "  \"timestamp_second\": " + info.simSecond + ",\n" +
                "  \"time\": \"" + String.format("%02d:%02d:%02d", h, m, s) + "\"\n" +
                "}\n" +
                "}";
    }
}
