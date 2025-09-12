package utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import config.ProtocolConstants;
import protocol.ResponseBuilder;

/**
 * Utility class for geospatial operations including coordinate validation,
 * encoding/decoding, distance calculations, and Redis protocol formatting.
 * 
 * This class provides comprehensive support for Redis GEO commands with
 * proper separation of concerns across validation, computation, and formatting.
 */
public final class GeoUtils {

    private GeoUtils() {
        // Utility class - prevent instantiation
    }

    // ==== Geographic Constants ====
    private static final double MIN_LATITUDE = -85.05112878;  // Web Mercator limit
    private static final double MAX_LATITUDE = 85.05112878;   // Web Mercator limit
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    // ==== Unit System ====
    public enum GeoUnit {
        METERS("m", 1.0),
        KILOMETERS("km", 1000.0),
        MILES("mi", 1609.344),
        FEET("ft", 0.3048);

        private final String unit;
        private final double metersFactor;

        GeoUnit(final String unit, final double metersFactor) {
            this.unit = unit;
            this.metersFactor = metersFactor;
        }

        public String getUnit() {
            return unit;
        }

        public double toMeters(final double value) {
            return value * metersFactor;
        }

        public double fromMeters(final double meters) {
            return meters / metersFactor;
        }

        public static GeoUnit fromString(final String unitStr) {
            if (unitStr == null) return null;
            
            for (GeoUnit unit : values()) {
                if (unit.unit.equalsIgnoreCase(unitStr)) {
                    return unit;
                }
            }
            return null;
        }
    }

    // ==== Data Structures ====
    
    /**
     * Represents a geospatial entry with coordinates and member name
     */
    public record GeoEntry(double longitude, double latitude, String member) {
        public GeoEntry {
            if (!isValidCoordinates(longitude, latitude)) {
                throw new IllegalArgumentException("Invalid coordinates: " + longitude + ", " + latitude);
            }
        }
    }

    /**
     * Parameters for GEOSEARCH command parsing
     */
    public record GeoSearchParams(
            boolean isFromMember,
            String memberName,
            double longitude,
            double latitude,
            double radiusMeters) {
    }

    // ==== Coordinate Validation ====
    
    public static boolean isValidLongitude(final double longitude) {
        return longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
    }

    public static boolean isValidLatitude(final double latitude) {
        return latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE;
    }

    public static boolean isValidCoordinates(final double longitude, final double latitude) {
        return isValidLongitude(longitude) && isValidLatitude(latitude);
    }

    // ==== Command Parsing ====
    
    /**
     * Parse GEOSEARCH command arguments into structured parameters.
     * Supports Redis syntax: GEOSEARCH key [FROMMEMBER member | FROMLONLAT lon lat] BYRADIUS radius unit
     */
    public static GeoSearchParams parseGeoSearchCommand(final String[] args) {
        try {
            // Find FROM clause
            String fromType = null;
            int fromIndex = -1;
            for (int i = 2; i < args.length; i++) {
                final String arg = args[i].toUpperCase();
                if ("FROMMEMBER".equals(arg) || "FROMLONLAT".equals(arg)) {
                    fromType = arg;
                    fromIndex = i;
                    break;
                }
            }

            if (fromType == null) return null;

            // Find BY clause
            int byIndex = -1;
            for (int i = fromIndex + 1; i < args.length; i++) {
                if ("BYRADIUS".equalsIgnoreCase(args[i])) {
                    byIndex = i;
                    break;
                }
            }

            if (byIndex == -1) return null;

            // Parse FROM parameters
            final boolean isFromMember = "FROMMEMBER".equals(fromType);
            String memberName = null;
            double longitude = 0, latitude = 0;

            if (isFromMember) {
                if (fromIndex + 1 >= byIndex) return null;
                memberName = args[fromIndex + 1];
            } else { // FROMLONLAT
                if (fromIndex + 2 >= byIndex) return null;
                longitude = Double.parseDouble(args[fromIndex + 1]);
                latitude = Double.parseDouble(args[fromIndex + 2]);
                
                if (!isValidCoordinates(longitude, latitude)) {
                    return null;
                }
            }

            // Parse BY parameters
            if (byIndex + 2 >= args.length) return null;
            final double radius = Double.parseDouble(args[byIndex + 1]);
            final String unitStr = args[byIndex + 2];

            final GeoUnit unit = GeoUnit.fromString(unitStr);
            if (unit == null || radius <= 0) return null;

            final double radiusMeters = unit.toMeters(radius);

            return new GeoSearchParams(isFromMember, memberName, longitude, latitude, radiusMeters);

        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Parse GEOADD command arguments into a list of GeoEntry objects.
     * Format: GEOADD key longitude latitude member [longitude latitude member ...]
     */
    public static List<GeoEntry> parseGeoEntries(final String[] args) {
        final List<GeoEntry> entries = new ArrayList<>();
        
        // Start from index 2 (after command and key), expect triplets
        for (int i = 2; i + 2 < args.length; i += 3) {
            try {
                final double longitude = Double.parseDouble(args[i]);
                final double latitude = Double.parseDouble(args[i + 1]);
                final String member = args[i + 2];
                
                entries.add(new GeoEntry(longitude, latitude, member));
            } catch (IllegalArgumentException e) {
                // Skip invalid entries rather than failing completely
                continue;
            }
        }
        
        return entries;
    }

    // ==== Redis Protocol Formatting ====
    
    /**
     * Format GEOPOS command response as RESP protocol.
     * Returns a list of coordinate arrays or null markers for missing members.
     */
    public static List<ByteBuffer> formatGeoPosResponse(
            final List<String> members,
            final Map<String, double[]> memberCoordinates) {

        final List<ByteBuffer> result = new ArrayList<>();
        
        for (final String member : members) {
            final double[] coordinates = memberCoordinates.get(member);
            if (coordinates == null) {
                // Member not found - return null bulk string
                result.add(ByteBuffer.wrap(ProtocolConstants.RESP_NULL_BULK_STRING.getBytes(StandardCharsets.UTF_8)));
            } else {
                // Return coordinate array [longitude, latitude]
                final List<String> coordStrings = List.of(
                        Double.toString(coordinates[0]),
                        Double.toString(coordinates[1]));
                result.add(ResponseBuilder.array(coordStrings));
            }
        }
        
        return result;
    }

    // ==== Distance Calculations ====
    
    /**
     * Calculate the great circle distance between two points using Haversine formula.
     * Returns distance in meters.
     */
    public static double calculateDistance(final double lon1, final double lat1,
                                         final double lon2, final double lat2) {
        final double radLat1 = Math.toRadians(lat1);
        final double radLat2 = Math.toRadians(lat2);
        final double deltaLat = radLat2 - radLat1;
        final double deltaLon = Math.toRadians(lon2 - lon1);

        final double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(radLat1) * Math.cos(radLat2) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
                
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

    // ==== Geospatial Encoding/Decoding ====
    
    /**
     * Encode longitude and latitude into a geohash score for sorted set storage.
     * Uses simplified bit interleaving for coordinate encoding.
     */
    public static double encodeGeohash(final double longitude, final double latitude) {
        // Normalize coordinates to positive range for bit operations
        final long longitudeEncoded = (long) ((longitude + 180.0) * 1_000_000);
        final long latitudeEncoded = (long) ((latitude + 90.0) * 1_000_000);
        
        return interleaveBits(longitudeEncoded, latitudeEncoded);
    }

    /**
     * Decode a geohash score back to longitude and latitude coordinates.
     */
    public static double[] decodeGeohash(final double score) {
        final long[] parts = deinterleaveBits((long) score);
        final double longitude = (parts[0] / 1_000_000.0) - 180.0;
        final double latitude = (parts[1] / 1_000_000.0) - 90.0;
        
        return new double[] { longitude, latitude };
    }

    /**
     * Interleave bits of two 32-bit integers to create a 64-bit Morton code.
     */
    private static double interleaveBits(final long x, final long y) {
        long result = 0;
        for (int i = 0; i < 32; i++) {
            result |= ((x >> i) & 1L) << (2 * i);
            result |= ((y >> i) & 1L) << (2 * i + 1);
        }
        return (double) result;
    }

    /**
     * De-interleave bits from a 64-bit Morton code back to two 32-bit values.
     */
    private static long[] deinterleaveBits(final long z) {
        long x = 0, y = 0;
        for (int i = 0; i < 32; i++) {
            x |= ((z >> (2 * i)) & 1L) << i;
            y |= ((z >> (2 * i + 1)) & 1L) << i;
        }
        return new long[] { x, y };
    }

    // ==== Spatial Calculations ====
    
    /**
     * Calculate bounding box coordinates for a circular search area.
     * Returns [minLon, minLat, maxLon, maxLat] array.
     */
    public static double[] calculateBoundingBox(final double longitude, final double latitude, 
                                               final double radiusMeters) {
        final double radianLatitude = Math.toRadians(latitude);
        final double deltaLatitude = Math.toDegrees(radiusMeters / EARTH_RADIUS_METERS);
        final double deltaLongitude = Math.toDegrees(radiusMeters / (EARTH_RADIUS_METERS * Math.cos(radianLatitude)));

        return new double[] {
                longitude - deltaLongitude,  // minLon
                latitude - deltaLatitude,    // minLat
                longitude + deltaLongitude,  // maxLon
                latitude + deltaLatitude     // maxLat
        };
    }
}
