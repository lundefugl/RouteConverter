/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.csv;

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.*;
import slash.navigation.simple.ColumbusV900Format;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static slash.navigation.csv.PositionProperty.*;
import static slash.navigation.csv.PositionProperty.Latitude;
import static slash.navigation.csv.PositionProperty.Latitude_NS;

/**
 * Reads and writes i-Blue 747 (.csv) files.
 *
 * Header: INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HEADING,DISTANCE,<br/>
 * Format: 3656,T,2010/12/09,10:59:05,SPS,28.649061,N,17.896196,W,513.863 M,15.862 km/h,178.240250,34.60 M,
 *
 * @author Christian Pesch
 */

public class iBlue747Format extends SimpleCsvBasedFormat<SimpleRoute> {
    protected static final Logger log = Logger.getLogger(iBlue747Format.class.getName());

    public String getName() {
        return "i-Blue 747 (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".csv";
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected boolean isPosition(Map<PositionProperty, Object> properties) {
        return (Boolean) properties.get(Valid);
    }

    protected Map<PositionProperty, Object> mergeValues(Map<PositionProperty, Object> properties) {
        String eastOrWest = (String) properties.get(Longitude_EW);
        if (eastOrWest != null) {
            Double longitude = (Double) properties.get(Longitude);
            if ("W".equals(eastOrWest) && longitude != null)
                longitude = -longitude;
            properties.remove(Longitude_EW);
            properties.put(Longitude, longitude);
        }
        String northOrSouth = (String) properties.get(Latitude_NS);
        if (northOrSouth != null) {
            Double latitude = (Double) properties.get(Latitude);
            if ("S".equals(northOrSouth) && latitude != null)
                latitude = -latitude;
            properties.remove(Latitude_NS);
            properties.put(Latitude, latitude);
        }
        return properties;
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    static {
        DATE_FORMAT.setTimeZone(CompactCalendar.UTC);
        TIME_FORMAT.setTimeZone(CompactCalendar.UTC);
    }

    private String formatTime(CompactCalendar time) {
        if (time == null)
            return "";
        return TIME_FORMAT.format(time.getTime());
    }

    private String formatDate(CompactCalendar date) {
        if (date == null)
            return "";
        return DATE_FORMAT.format(date.getTime());
    }

    protected List<Column> getColumns() {
        return Arrays.asList(
                new IndexColumn("INDEX"),
          //      new StringColumn("RCR", "T"),
          //      new DateColumn("DATE", DATE_FORMAT),
          //      new DateColumn("TIME", TIME_FORMAT),
          //      new StringColumn("VALID", "SPS"),
                new LatitudeColumn("LATITUDE"),
                new LatitudeNSColumn("N/S"),
                new LongitudeColumn("LONGITUDE"),
                new LongitudeEWColumn("E/W")
        );
    }
}
