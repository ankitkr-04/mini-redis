package utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import config.ProtocolConstants;
import protocol.ResponseBuilder;

/**
 * Utility class for GEO commands: encoding/decoding coordinates,
 * distance calculation, bounding boxes, and RESP formatting.
 */
public final class GeoUtils {

    private GeoUtils() {
    }

    // ==== Constants ====
    private static final double MIN_LAT = -85.05112878;
    private static final double MAX_LAT = 85.05112878;
    private static final double MIN_LON = -180.0;
    private static final double MAX_LON = 180.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public static final Map<String, Double> UNIT_TO_METERS = Map.of(
            "m", 1.0,
            "km", 1000.0,
            "mi", 1609.344,
            "ft", 0.3048);

    public static boolean isValidLongitude(final double lon) {
        return lon >= MIN_LON && lon <= MAX_LON;
    }

    public static boolean isValidLatitude(final double lat) {
        return lat >= MIN_LAT && lat <= MAX_LAT;
    }

    public static boolean isValidUnit(final String unit) {
        return UNIT_TO_METERS.containsKey(unit);
    }

    // ==== GEO Entry ====
    public static record GeoEntry(double lon, double lat, String member) {
    }

    /**
     * Parse command arguments into a list of GeoEntry.
     * Assumes args format: key lon lat member [lon lat member ...]
     */
    public static List<GeoEntry> parseGeoEntries(final String[] args) {
        final List<GeoEntry> entries = new ArrayList<>();
        for (int i = 2; i + 2 < args.length; i += 3) {
            final double lon = Double.parseDouble(args[i]);
            final double lat = Double.parseDouble(args[i + 1]);
            final String member = args[i + 2];
            entries.add(new GeoEntry(lon, lat, member));
        }
        return entries;
    }

    public static GEO_UNIT parseUnitOrNull(final String unitStr) {
        if (unitStr == null)
            return null;
        try {
            return GEO_UNIT.valueOf(unitStr.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts a value in the given unit to meters.
     */
    public static double convertToMeters(final double value, final GEO_UNIT unit) {
        final Double factor = unit.toMeters;

        return value * factor;
    }

    /**
     * Format a GEO command's POS response into a list of RESP ByteBuffers.
     * Null coordinates are returned as RESP nil.
     */
    public static List<ByteBuffer> formatGeoPosForResp(
            final List<String> members,
            final Map<String, double[]> memberCoords) {

        final List<ByteBuffer> result = new ArrayList<>();
        for (final String member : members) {
            final double[] lonLat = memberCoords.get(member);
            if (lonLat == null) {
                result.add(ByteBuffer.wrap(ProtocolConstants.RESP_NULL_BULK_STRING.getBytes(StandardCharsets.UTF_8)));
            } else {
                final List<String> coords = List.of(
                        Double.toString(lonLat[0]),
                        Double.toString(lonLat[1]));
                result.add(ResponseBuilder.array(coords));
            }
        }
        return result;
    }

    // ==== Distance Calculation ====
    /**
     * Haversine distance in meters between two coordinates.
     */
    public static double haversine(final double lon1, final double lat1,
            final double lon2, final double lat2) {
        final double radLat1 = Math.toRadians(lat1);
        final double radLat2 = Math.toRadians(lat2);
        final double dLat = radLat2 - radLat1;
        final double dLon = Math.toRadians(lon2 - lon1);

        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(radLat1) * Math.cos(radLat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

    // ==== Encoding / Decoding (Morton / Geohash simplified) ====
    public static double encodeGeoHash(final double lon, final double lat) {
        final long lonEnc = (long) ((lon + 180.0) * 1_000_000);
        final long latEnc = (long) ((lat + 90.0) * 1_000_000);
        return interleaveBits(lonEnc, latEnc);
    }

    public static double[] decodeGeoHash(final double score) {
        final long[] parts = deinterleaveBits((long) score);
        final double lon = (parts[0] / 1_000_000.0) - 180.0;
        final double lat = (parts[1] / 1_000_000.0) - 90.0;
        return new double[] { lon, lat };
    }

    private static double interleaveBits(final long x, final long y) {
        long z = 0;
        for (int i = 0; i < 32; i++) {
            z |= ((x >> i) & 1L) << (2 * i);
            z |= ((y >> i) & 1L) << (2 * i + 1);
        }
        return (double) z;
    }

    private static long[] deinterleaveBits(final long z) {
        long x = 0, y = 0;
        for (int i = 0; i < 32; i++) {
            x |= ((z >> (2 * i)) & 1L) << i;
            y |= ((z >> (2 * i + 1)) & 1L) << i;
        }
        return new long[] { x, y };
    }

    // ==== Bounding Box (for GEOSEARCH) ====
    public static double[] boundingBox(final double lon, final double lat, final double radiusMeters) {
        final double radLat = Math.toRadians(lat);
        final double deltaLat = Math.toDegrees(radiusMeters / EARTH_RADIUS_METERS);
        final double deltaLon = Math.toDegrees(radiusMeters / (EARTH_RADIUS_METERS * Math.cos(radLat)));

        return new double[] {
                lon - deltaLon, lat - deltaLat, // minLon, minLat
                lon + deltaLon, lat + deltaLat // maxLon, maxLat
        };
    }

    // ==== GEO Units ====
    public enum GEO_UNIT {
        M("m", 1.0),
        KM("km", 1000.0),
        MI("mi", 1609.344),
        FT("ft", 0.3048);

        public final String unit;
        public final double toMeters;

        GEO_UNIT(final String unit, final double toMeters) {
            this.unit = unit;
            this.toMeters = toMeters;
        }
    }

    public static double convertDistance(final double meters, final GEO_UNIT unit) {
        return meters * unit.toMeters;
    }

}
