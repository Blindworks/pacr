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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses GPX 1.1 files into a {@link ParsedActivityData} container.
 * Uses only the JDK's built-in XML APIs — no additional Maven dependencies required.
 */
@Service
public class GpxParsingService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Parses raw GPX bytes and populates a {@link ParsedActivityData} with all extractable metrics.
     *
     * @param gpxBytes raw file content
     * @return parsed activity data; never null
     * @throws Exception if the XML is malformed or otherwise unreadable
     */
    public ParsedActivityData parse(byte[] gpxBytes) throws Exception {
        Document doc = buildDocument(gpxBytes);
        doc.getDocumentElement().normalize();

        CompletedTraining training = new CompletedTraining();
        training.setSource("GPX");

        List<Integer> timeSecondsList = new ArrayList<>();
        List<Integer> heartRatesList = new ArrayList<>();
        List<double[]> latLngPointsList = new ArrayList<>();

        // Sport from <type> element (direct child of <trk>)
        NodeList typeNodes = doc.getElementsByTagName("type");
        if (typeNodes.getLength() > 0) {
            String sportRaw = typeNodes.item(0).getTextContent().trim();
            training.setSport(sportRaw.toLowerCase());
        }

        // Start time from <metadata><time> — fall back to first trkpt time below
        String startTimeRaw = null;
        NodeList metadataTimeNodes = doc.getElementsByTagName("metadata");
        if (metadataTimeNodes.getLength() > 0) {
            Element metadata = (Element) metadataTimeNodes.item(0);
            NodeList timeNodes = metadata.getElementsByTagName("time");
            if (timeNodes.getLength() > 0) {
                startTimeRaw = timeNodes.item(0).getTextContent().trim();
            }
        }

        // Iterate all <trkpt> nodes
        NodeList trkpts = doc.getElementsByTagName("trkpt");
        int pointCount = trkpts.getLength();

        double totalDistanceM = 0.0;
        double elevationGainM = 0.0;
        double elevationLossM = 0.0;

        Double prevLat = null;
        Double prevLon = null;
        Double prevEle = null;

        Double firstLat = null;
        Double firstLon = null;
        Double lastLat = null;
        Double lastLon = null;

        Long firstTimestamp = null;   // epoch seconds of first trkpt
        Long prevTimestamp = null;

        int hrSum = 0;
        int hrCount = 0;
        int hrMax = 0;
        int hrMin = Integer.MAX_VALUE;

        for (int i = 0; i < pointCount; i++) {
            Element trkpt = (Element) trkpts.item(i);

            double lat = Double.parseDouble(trkpt.getAttribute("lat"));
            double lon = Double.parseDouble(trkpt.getAttribute("lon"));
            latLngPointsList.add(new double[]{lat, lon});

            // Elevation
            Double ele = null;
            NodeList eleNodes = trkpt.getElementsByTagName("ele");
            if (eleNodes.getLength() > 0) {
                ele = Double.parseDouble(eleNodes.item(0).getTextContent().trim());
            }

            // Timestamp from <time>
            Long currentTimestamp = null;
            NodeList trkptTimeNodes = trkpt.getElementsByTagName("time");
            if (trkptTimeNodes.getLength() > 0) {
                String timeText = trkptTimeNodes.item(0).getTextContent().trim();
                currentTimestamp = parseIsoToEpochSeconds(timeText);
                if (firstTimestamp == null) {
                    firstTimestamp = currentTimestamp;
                    // Use first trkpt time as start time if metadata had none
                    if (startTimeRaw == null) {
                        startTimeRaw = timeText;
                    }
                }
            }

            // Heart rate from <gpxtpx:hr> or <ns3:hr>
            Integer hr = null;
            NodeList hrNodes = trkpt.getElementsByTagName("gpxtpx:hr");
            if (hrNodes.getLength() == 0) {
                hrNodes = trkpt.getElementsByTagName("ns3:hr");
            }
            if (hrNodes.getLength() == 0) {
                // Garmin Connect exports sometimes use un-prefixed hr inside extensions
                hrNodes = trkpt.getElementsByTagName("hr");
            }
            if (hrNodes.getLength() > 0) {
                hr = Integer.parseInt(hrNodes.item(0).getTextContent().trim());
                hrSum += hr;
                hrCount++;
                if (hr > hrMax) hrMax = hr;
                if (hr < hrMin) hrMin = hr;
            }

            // Accumulate distance via Haversine
            if (prevLat != null) {
                totalDistanceM += haversineMeters(prevLat, prevLon, lat, lon);
            }

            // Elevation gain / loss
            if (prevEle != null && ele != null) {
                double diff = ele - prevEle;
                if (diff > 0) elevationGainM += diff;
                else elevationLossM += (-diff);
            }

            // GPS start/end
            if (firstLat == null) {
                firstLat = lat;
                firstLon = lon;
            }
            lastLat = lat;
            lastLon = lon;

            // Build time/HR streams (relative seconds from first trkpt)
            int relativeSeconds = 0;
            if (firstTimestamp != null && currentTimestamp != null) {
                relativeSeconds = (int) (currentTimestamp - firstTimestamp);
            } else {
                relativeSeconds = i; // fallback: 1 sample per second
            }
            timeSecondsList.add(relativeSeconds);
            heartRatesList.add(hr); // may be null

            prevLat = lat;
            prevLon = lon;
            prevEle = ele;
            prevTimestamp = currentTimestamp;
        }

        // Derive duration from first/last timestamp
        int durationSeconds = 0;
        if (firstTimestamp != null && prevTimestamp != null) {
            durationSeconds = (int) (prevTimestamp - firstTimestamp);
        }

        // Populate training fields
        training.setDistanceKm(totalDistanceM / 1000.0);
        training.setDurationSeconds(durationSeconds > 0 ? durationSeconds : null);
        training.setMovingTimeSeconds(durationSeconds > 0 ? durationSeconds : null);
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

        if (hrCount > 0) {
            training.setAverageHeartRate(hrSum / hrCount);
            training.setMaxHeartRate(hrMax);
            training.setMinHeartRate(hrMin);
        }

        // Pace: totalDuration / distanceKm
        double distanceKm = totalDistanceM / 1000.0;
        if (durationSeconds > 0 && distanceKm > 0) {
            training.setAveragePaceSecondsPerKm((int) (durationSeconds / distanceKm));
        }

        // Training date and start time from start time
        if (startTimeRaw != null) {
            LocalDate date = parseIsoToLocalDate(startTimeRaw);
            if (date != null) {
                training.setTrainingDate(date);
            }
            java.time.LocalTime time = parseIsoToLocalTime(startTimeRaw);
            if (time != null) {
                training.setStartTime(time);
            }
        }

        ParsedActivityData result = new ParsedActivityData();
        result.training = training;
        result.timeSeconds = timeSecondsList;
        result.heartRates = heartRatesList;
        result.latLngPoints = latLngPointsList;
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Document buildDocument(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Namespace-aware so we can use getElementsByTagNameNS; but we also
        // fall back to simple tag-name lookups that handle prefixed elements.
        factory.setNamespaceAware(true);
        // Disable external entity processing for security
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(bytes));
    }

    /**
     * Haversine formula — returns distance in metres between two WGS-84 coordinates.
     */
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

    /**
     * Parses an ISO-8601 date-time string (e.g. "2024-06-01T08:30:00Z") to epoch seconds.
     * Returns null if parsing fails.
     */
    private Long parseIsoToEpochSeconds(String text) {
        try {
            // Replace trailing 'Z' with '+00:00' for OffsetDateTime compatibility
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
}
