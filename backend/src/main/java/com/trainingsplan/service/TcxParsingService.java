package com.trainingsplan.service;

import com.trainingsplan.entity.CompletedTraining;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses TCX (Training Center XML) files into a {@link ParsedActivityData} container.
 * Uses only the JDK's built-in XML APIs — no additional Maven dependencies required.
 *
 * <p>TCX structure relevant here:
 * <pre>
 *   &lt;TrainingCenterDatabase&gt;
 *     &lt;Activities&gt;
 *       &lt;Activity Sport="Running"&gt;
 *         &lt;Id&gt;2024-06-01T08:00:00Z&lt;/Id&gt;
 *         &lt;Lap StartTime="..."&gt;
 *           &lt;TotalTimeSeconds&gt;...&lt;/TotalTimeSeconds&gt;
 *           &lt;DistanceMeters&gt;...&lt;/DistanceMeters&gt;
 *           &lt;Calories&gt;...&lt;/Calories&gt;
 *           &lt;MaximumHeartRateBpm&gt;&lt;Value&gt;...&lt;/Value&gt;&lt;/MaximumHeartRateBpm&gt;
 *           &lt;Track&gt;
 *             &lt;Trackpoint&gt;
 *               &lt;Time&gt;...&lt;/Time&gt;
 *               &lt;Position&gt;&lt;LatitudeDegrees&gt;...&lt;/LatitudeDegrees&gt;&lt;LongitudeDegrees&gt;...&lt;/LongitudeDegrees&gt;&lt;/Position&gt;
 *               &lt;AltitudeMeters&gt;...&lt;/AltitudeMeters&gt;
 *               &lt;HeartRateBpm&gt;&lt;Value&gt;...&lt;/Value&gt;&lt;/HeartRateBpm&gt;
 *             &lt;/Trackpoint&gt;
 *           &lt;/Track&gt;
 *         &lt;/Lap&gt;
 *       &lt;/Activity&gt;
 *     &lt;/Activities&gt;
 *   &lt;/TrainingCenterDatabase&gt;
 * </pre>
 */
@Service
public class TcxParsingService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * Parses raw TCX bytes and returns a populated {@link ParsedActivityData}.
     *
     * @param tcxBytes raw file content
     * @return parsed activity data; never null
     * @throws Exception if the XML is malformed or otherwise unreadable
     */
    public ParsedActivityData parse(byte[] tcxBytes) throws Exception {
        Document doc = buildDocument(tcxBytes);
        doc.getDocumentElement().normalize();

        CompletedTraining training = new CompletedTraining();
        training.setSource("TCX");

        List<Integer> timeSecondsList = new ArrayList<>();
        List<Integer> heartRatesList = new ArrayList<>();

        // Sport from <Activity Sport="..."> attribute
        NodeList activityNodes = doc.getElementsByTagName("Activity");
        if (activityNodes.getLength() > 0) {
            Element activity = (Element) activityNodes.item(0);
            String sport = activity.getAttribute("Sport");
            if (sport != null && !sport.isBlank()) {
                training.setSport(sport.toLowerCase());
            }

            // Start time / training date from <Id>
            NodeList idNodes = activity.getElementsByTagName("Id");
            if (idNodes.getLength() > 0) {
                String idText = idNodes.item(0).getTextContent().trim();
                LocalDate date = parseIsoToLocalDate(idText);
                if (date != null) {
                    training.setTrainingDate(date);
                }
                java.time.LocalTime time = parseIsoToLocalTime(idText);
                if (time != null) {
                    training.setStartTime(time);
                }
            }
        }

        // Accumulate Lap-level totals
        double totalTimeSeconds = 0.0;
        double totalDistanceMeters = 0.0;
        int totalCalories = 0;
        int lapMaxHr = 0;

        NodeList lapNodes = doc.getElementsByTagName("Lap");
        for (int l = 0; l < lapNodes.getLength(); l++) {
            Element lap = (Element) lapNodes.item(l);

            totalTimeSeconds  += doubleTextOf(lap, "TotalTimeSeconds");
            totalDistanceMeters += doubleTextOf(lap, "DistanceMeters");
            totalCalories     += (int) doubleTextOf(lap, "Calories");

            // MaximumHeartRateBpm contains a child <Value>
            NodeList maxHrNodes = lap.getElementsByTagName("MaximumHeartRateBpm");
            if (maxHrNodes.getLength() > 0) {
                Element maxHrEl = (Element) maxHrNodes.item(0);
                int lapMax = (int) doubleTextOf(maxHrEl, "Value");
                if (lapMax > lapMaxHr) lapMaxHr = lapMax;
            }
        }

        // Populate summary fields from accumulated lap data
        if (totalDistanceMeters > 0) {
            training.setDistanceKm(totalDistanceMeters / 1000.0);
        }
        if (totalTimeSeconds > 0) {
            training.setDurationSeconds((int) totalTimeSeconds);
            training.setMovingTimeSeconds((int) totalTimeSeconds);
        }
        if (totalCalories > 0) {
            training.setCalories(totalCalories);
        }
        if (lapMaxHr > 0) {
            training.setMaxHeartRate(lapMaxHr);
        }

        // Process all <Trackpoint> elements for GPS track, elevation, and HR stream
        NodeList trackpoints = doc.getElementsByTagName("Trackpoint");
        int pointCount = trackpoints.getLength();

        double elevationGainM = 0.0;
        double elevationLossM = 0.0;

        Double prevLat = null;
        Double prevLon = null;
        Double prevEle = null;

        Double firstLat = null;
        Double firstLon = null;
        Double lastLat = null;
        Double lastLon = null;

        Long firstTimestamp = null;
        Long prevTimestamp = null;

        int hrSum = 0;
        int hrCount = 0;
        int hrMin = Integer.MAX_VALUE;

        for (int i = 0; i < pointCount; i++) {
            Element tp = (Element) trackpoints.item(i);

            // Timestamp
            Long currentTimestamp = null;
            NodeList timeNodes = tp.getElementsByTagName("Time");
            if (timeNodes.getLength() > 0) {
                currentTimestamp = parseIsoToEpochSeconds(timeNodes.item(0).getTextContent().trim());
                if (firstTimestamp == null) {
                    firstTimestamp = currentTimestamp;
                }
            }

            // GPS position
            Double lat = null;
            Double lon = null;
            NodeList posNodes = tp.getElementsByTagName("Position");
            if (posNodes.getLength() > 0) {
                Element pos = (Element) posNodes.item(0);
                lat = doubleTextOrNull(pos, "LatitudeDegrees");
                lon = doubleTextOrNull(pos, "LongitudeDegrees");
            }

            // Altitude
            Double ele = doubleTextOrNull(tp, "AltitudeMeters");

            // Elevation gain/loss
            if (prevEle != null && ele != null) {
                double diff = ele - prevEle;
                if (diff > 0) elevationGainM += diff;
                else elevationLossM += (-diff);
            }

            // GPS start/end
            if (lat != null && lon != null) {
                if (firstLat == null) {
                    firstLat = lat;
                    firstLon = lon;
                }
                lastLat = lat;
                lastLon = lon;
            }

            // Heart rate from <HeartRateBpm><Value>
            Integer hr = null;
            NodeList hrNodes = tp.getElementsByTagName("HeartRateBpm");
            if (hrNodes.getLength() > 0) {
                Element hrEl = (Element) hrNodes.item(0);
                Double hrVal = doubleTextOrNull(hrEl, "Value");
                if (hrVal != null) {
                    hr = hrVal.intValue();
                    hrSum += hr;
                    hrCount++;
                    if (hr < hrMin) hrMin = hr;
                }
            }

            // Build stream (relative seconds from first trackpoint)
            int relativeSeconds = 0;
            if (firstTimestamp != null && currentTimestamp != null) {
                relativeSeconds = (int) (currentTimestamp - firstTimestamp);
            } else {
                relativeSeconds = i;
            }
            timeSecondsList.add(relativeSeconds);
            heartRatesList.add(hr); // may be null

            prevLat = lat;
            prevLon = lon;
            prevEle = ele;
            prevTimestamp = currentTimestamp;
        }

        // Overwrite with trackpoint-derived duration if lap data was absent
        if (training.getDurationSeconds() == null && firstTimestamp != null && prevTimestamp != null) {
            int dur = (int) (prevTimestamp - firstTimestamp);
            training.setDurationSeconds(dur);
            training.setMovingTimeSeconds(dur);
        }

        // Elevation
        training.setElevationGainM((int) elevationGainM);
        training.setElevationLossM((int) elevationLossM);
        training.setTotalGpsPoints(pointCount);

        if (firstLat != null) {
            training.setStartLatitude(firstLat);
            training.setStartLongitude(firstLon);
        }
        if (lastLat != null) {
            training.setEndLatitude(lastLat);
            training.setEndLongitude(lastLon);
        }

        // Average HR from stream if not already set
        if (hrCount > 0) {
            training.setAverageHeartRate(hrSum / hrCount);
            if (hrMin < Integer.MAX_VALUE) training.setMinHeartRate(hrMin);
        }

        // Pace
        double distanceKm = training.getDistanceKm() != null ? training.getDistanceKm() : 0.0;
        Integer duration  = training.getDurationSeconds();
        if (duration != null && duration > 0 && distanceKm > 0) {
            training.setAveragePaceSecondsPerKm((int) (duration / distanceKm));
        }

        ParsedActivityData result = new ParsedActivityData();
        result.training = training;
        result.timeSeconds = timeSecondsList;
        result.heartRates = heartRatesList;
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Document buildDocument(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(bytes));
    }

    /**
     * Returns the double value of the first child element matching {@code tagName},
     * or 0.0 if absent or unparseable.
     */
    private double doubleTextOf(Element parent, String tagName) {
        Double val = doubleTextOrNull(parent, tagName);
        return val != null ? val : 0.0;
    }

    /**
     * Returns the double value of the first child element matching {@code tagName},
     * or null if absent or unparseable.
     */
    private Double doubleTextOrNull(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        String text = nodes.item(0).getTextContent().trim();
        if (text.isEmpty()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses an ISO-8601 date-time string (e.g. "2024-06-01T08:00:00Z") to epoch seconds.
     * Returns null if parsing fails.
     */
    private Long parseIsoToEpochSeconds(String text) {
        try {
            String normalized = text.replace("Z", "+00:00");
            return java.time.OffsetDateTime.parse(normalized).toEpochSecond();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parses an ISO-8601 date-time string to a {@link LocalDate}.
     * Returns null if parsing fails.
     */
    private LocalDate parseIsoToLocalDate(String text) {
        try {
            String normalized = text.replace("Z", "+00:00");
            return java.time.OffsetDateTime.parse(normalized).toLocalDate();
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(text.substring(0, 10));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Parses an ISO-8601 date-time string to a {@link java.time.LocalTime}.
     * Returns null if parsing fails or the string contains no time component.
     */
    private java.time.LocalTime parseIsoToLocalTime(String text) {
        try {
            String normalized = text.replace("Z", "+00:00");
            return java.time.OffsetDateTime.parse(normalized).toLocalTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Haversine formula — returns distance in metres between two WGS-84 coordinates.
     * Kept for potential future use (e.g. if DistanceMeters is missing from TCX laps).
     */
    @SuppressWarnings("unused")
    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
