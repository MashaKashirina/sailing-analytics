package com.sap.sailing.declination.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.TimePoint;

public class WMMFileTest {
    @Test
    public void testWMM2025Reading() throws IOException {
        final Geomagnetism g = new Geomagnetism(new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("WMM2025.COF"))));
        assertNotNull(g);
    }

    @Test
    public void testWMM2025Content() throws IOException, ParseException {
        final Geomagnetism g = new Geomagnetism(new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("WMMHR2025.COF"))));
        final double lat = 44.123;
        final double lng = 8.234;
        final GregorianCalendar cal = new GregorianCalendar(2025, 10, 4, 0, 46, 9);
        g.calculate(lng, lat, /* altitude */ 0, cal);
        final double declinationWMM2025 = g.getDeclination();
        final DeclinationService s = DeclinationService.INSTANCE;
        final Declination declinationFromService = s.getDeclination(TimePoint.of(cal.getTimeInMillis()), new DegreePosition(lat, lng), /* timeout */ 10000);
        assertEquals(declinationWMM2025, declinationFromService.getBearing().getDegrees());
    }
}
