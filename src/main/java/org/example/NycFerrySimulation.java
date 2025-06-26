package org.example;

import java.util.*;

public class NycFerrySimulation {

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
        Route route;
        List<Integer> startTimesSec; // schedule in seconds from midnight
        Ferry(String name, Route route, List<Integer> startTimesSec) {
            this.name = name; this.route = route; this.startTimesSec = startTimesSec;
        }
    }

    static final Map<String, Coordinate> STOPS = new HashMap<>();
    static {
        STOPS.put("Staten Island", new Coordinate(-74.073, 40.643));
        STOPS.put("Manhattan South", new Coordinate(-74.011, 40.701));
        STOPS.put("Brooklyn", new Coordinate(-73.991, 40.700));
        STOPS.put("Queens", new Coordinate(-73.937, 40.767));
        STOPS.put("Governors Island", new Coordinate(-74.016, 40.689));
        STOPS.put("East 34th", new Coordinate(-73.972, 40.745));
        STOPS.put("New Jersey", new Coordinate(-74.060, 40.730));
        STOPS.put("Liberty Island", new Coordinate(-74.0445, 40.6892));
        STOPS.put("Ellis Island", new Coordinate(-74.0396, 40.6995));
        STOPS.put("Wall Street", new Coordinate(-74.009, 40.705));
    }

    static final Map<String, Integer> TRAVEL_TIMES_SEC = new HashMap<>();
    static {
        TRAVEL_TIMES_SEC.put("Staten Island-Manhattan South", 25 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-Staten Island", 25 * 60);
        TRAVEL_TIMES_SEC.put("Brooklyn-Manhattan South", 15 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-Brooklyn", 15 * 60);
        TRAVEL_TIMES_SEC.put("Queens-East 34th", 20 * 60);
        TRAVEL_TIMES_SEC.put("East 34th-Queens", 20 * 60);
        TRAVEL_TIMES_SEC.put("Governors Island-Manhattan South", 7 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-Governors Island", 7 * 60);
        TRAVEL_TIMES_SEC.put("Manhattan South-New Jersey", 30 * 60);
        TRAVEL_TIMES_SEC.put("New Jersey-Manhattan South", 30 * 60);
        TRAVEL_TIMES_SEC.put("Wall Street-Governors Island", 8 * 60);
        TRAVEL_TIMES_SEC.put("Governors Island-Wall Street", 8 * 60);
        TRAVEL_TIMES_SEC.put("Liberty Island-Ellis Island", 10 * 60);
        TRAVEL_TIMES_SEC.put("Ellis Island-Liberty Island", 10 * 60);
        TRAVEL_TIMES_SEC.put("Wall Street-Liberty Island", 12 * 60);
        TRAVEL_TIMES_SEC.put("Liberty Island-Wall Street", 12 * 60);
    }

    static final List<Route> ROUTES = new ArrayList<>();
    static {
        ROUTES.add(new Route("Staten Island - Manhattan",
                Arrays.asList("Staten Island", "Manhattan South")));
        ROUTES.add(new Route("Brooklyn - Manhattan",
                Arrays.asList("Brooklyn", "Manhattan South")));
        ROUTES.add(new Route("Queens - Manhattan",
                Arrays.asList("Queens", "East 34th")));
        ROUTES.add(new Route("Governors Island - Manhattan",
                Arrays.asList("Governors Island", "Manhattan South")));
        ROUTES.add(new Route("Manhattan - New Jersey",
                Arrays.asList("Manhattan South", "New Jersey")));
        ROUTES.add(new Route("Wall Street - Governors Island",
                Arrays.asList("Wall Street", "Governors Island")));
        ROUTES.add(new Route("Liberty Island - Ellis Island",
                Arrays.asList("Liberty Island", "Ellis Island")));
        ROUTES.add(new Route("Wall Street - Liberty Island",
                Arrays.asList("Wall Street", "Liberty Island")));
    }

    static List<Integer> generateTimesSec(int startMin, int intervalMin, int count) {
        List<Integer> times = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            times.add((startMin + i * intervalMin) * 60);
        }
        return times;
    }

    static final List<Ferry> FERRIES = new ArrayList<>();
    static {
        FERRIES.add(new Ferry("FerryA", ROUTES.get(0), generateTimesSec(0, 60, 20)));
        FERRIES.add(new Ferry("FerryB", ROUTES.get(1), generateTimesSec(15, 60, 20)));
        FERRIES.add(new Ferry("FerryC", ROUTES.get(2), generateTimesSec(30, 60, 20)));
        FERRIES.add(new Ferry("FerryD", ROUTES.get(3), generateTimesSec(10, 40, 20)));
        FERRIES.add(new Ferry("FerryE", ROUTES.get(4), generateTimesSec(5, 50, 15)));
        FERRIES.add(new Ferry("FerryF", ROUTES.get(5), generateTimesSec(20, 45, 15)));
        FERRIES.add(new Ferry("FerryG", ROUTES.get(6), generateTimesSec(0, 90, 10)));
        FERRIES.add(new Ferry("FerryH", ROUTES.get(7), generateTimesSec(10, 75, 12)));
    }

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
        String ferryName, routeName, segment, direction;
        int simSecond;
        CoordinateAndInfo(Coordinate c, String ferryName, String routeName,
                          String segment, String direction, int simSecond) {
            this.coord = c; this.ferryName = ferryName; this.routeName = routeName;
            this.segment = segment; this.direction = direction; this.simSecond = simSecond;
        }
    }

    static CoordinateAndInfo getFerryPosition(Ferry ferry, int simSecond) {
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
                        return new CoordinateAndInfo(coord, ferry.name, ferry.route.name,
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
                        return new CoordinateAndInfo(coord, ferry.name, ferry.route.name,
                                stops.get(i) + "->" + stops.get(i - 1), "backward", simSecond);
                    }
                    acc += segTime;
                }
            }
        }
        return null;
    }

    static String fmtDouble(double d) {
        return String.format(Locale.US, "%.6f", d);
    }

    static String toGeoJSONFeature(CoordinateAndInfo info) {
        int h = info.simSecond / 3600;
        int m = (info.simSecond % 3600) / 60;
        int s = info.simSecond % 60;
        return "{\n" +
                "\"type\": \"Feature\",\n" +
                "\"geometry\": {\n" +
                "  \"type\": \"Point\",\n" +
                "  \"coordinates\": [" + fmtDouble(info.coord.lon) + ", " + fmtDouble(info.coord.lat) + "]\n" +
                "},\n" +
                "\"properties\": {\n" +
                "  \"ferry_name\": \"" + info.ferryName + "\",\n" +
                "  \"route\": \"" + info.routeName + "\",\n" +
                "  \"segment\": \"" + info.segment + "\",\n" +
                "  \"direction\": \"" + info.direction + "\",\n" +
                "  \"timestamp_second\": " + info.simSecond + ",\n" +
                "  \"time\": \"" + String.format("%02d:%02d:%02d", h, m, s) + "\"\n" +
                "}\n" +
                "}";
    }

    static String toCatalogExplorerTrackUpdate(CoordinateAndInfo info) {
        int h = info.simSecond / 3600;
        int m = (info.simSecond % 3600) / 60;
        int s = info.simSecond % 60;
        return "{\n" +
                "\"action\": \"PUT\",\n" +
                "\"geometry\": {\n" +
                "  \"type\": \"Point\",\n" +
                "  \"coordinates\": [" + fmtDouble(info.coord.lon) + ", " + fmtDouble(info.coord.lat) + "]\n" +
                "},\n" +
                "\"id\": \""+ info.ferryName +"\",\n" +
                "\"properties\": {\n" +
                "  \"ferry_name\": \"" + info.ferryName + "\",\n" +
                "  \"route\": \"" + info.routeName + "\",\n" +
                "  \"segment\": \"" + info.segment + "\",\n" +
                "  \"direction\": \"" + info.direction + "\",\n" +
                "  \"timestamp_second\": " + info.simSecond + ",\n" +
                "  \"time\": \"" + String.format("%02d:%02d:%02d", h, m, s) + "\"\n" +
                "}\n" +
                "}";
    }

}
