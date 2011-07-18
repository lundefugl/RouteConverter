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
import slash.navigation.base.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.navigation.csv.PositionProperty.*;

/**
 * Represents simple CSV based text route formats.
 *
 * @author Christian Pesch
 */

public abstract class SimpleCsvBasedFormat<R extends SimpleRoute> extends SimpleFormat<R> {
    protected static final Logger log = Logger.getLogger(SimpleCsvBasedFormat.class.getName());

    protected static final char SEPARATOR = ',';

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings("unchecked")
    protected R createRoute(RouteCharacteristics characteristics, List<Wgs84Position> positions) {
        return (R)new Wgs84Route(this, characteristics, positions);
    }

    @SuppressWarnings("unchecked")
    public List<R> read(BufferedReader reader, CompactCalendar startDate, String encoding) throws IOException {
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();

        int lineCount = 0;
        List<PositionProperty> positionProperties = new ArrayList<PositionProperty>(0);
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.length() == 0)
                continue;

            String[] values = line.split(Character.toString(SEPARATOR));

            // try to read header definition
            if (positionProperties.size() == 0) {
                positionProperties = readHeader(values);

                // a header has to be defined in the first five lines of a file
                if (lineCount++ > 5)
                    return null;

                continue;
            }

            // read position from line
            Map<PositionProperty, Object> valueMap = new HashMap<PositionProperty, Object>();
            for (int i = 0; i < values.length; i++) {
                Object aValue = parseValue(values[i], positionProperties.get(i));
                valueMap.put(positionProperties.get(i), aValue);
            }

            if (isPosition(valueMap)) {
                valueMap = mergeValues(valueMap);

                // create position from type
                // TODO what about other properties?
                positions.add(new Wgs84Position(
                        (Double) valueMap.get(Longitude),
                        (Double) valueMap.get(Latitude),
                        (Double) valueMap.get(Elevation),
                        (Double) valueMap.get(Speed),
                        (CompactCalendar) valueMap.get(Time),
                        (String) valueMap.get(Comment))
                );
            }
        }

        if (positions.size() > 0)
            return asList(createRoute(getRouteCharacteristics(), positions));
        else
            return null;
    }

    private List<PositionProperty> readHeader(String[] values) {
        List<PositionProperty> properties = new ArrayList<PositionProperty>(values.length);

        // map header values to position properties
        for (String value : values) {
            PositionProperty property = determinePositionProperty(value);
            properties.add(property);
        }

        // need at least longitude and latitude
        if (!containsPositionProperty(properties, Longitude) &&
                !containsPositionProperty(properties, Latitude)) {
            properties.clear();
        }

        return properties;
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Waypoints;
    }

    private boolean containsPositionProperty(List<PositionProperty> positionProperties, PositionProperty find) {
        for (PositionProperty positionProperty : positionProperties) {
            if (positionProperty.equals(find))
                return true;
        }
        return false;
    }

    protected abstract List<Column> getColumns();

    protected boolean isPosition(Map<PositionProperty, Object> properties) {
        return true;
    }

    protected Map<PositionProperty, Object> mergeValues(Map<PositionProperty, Object> properties) {
        return properties;
    }

    private Object parseValue(String string, PositionProperty positionProperty) {
        // TODO choose more elegant and robust way for mapping
        if (positionProperty.equals(Longitude) || positionProperty.equals(Latitude)) {
            return parseDouble(string);
        }
        if (positionProperty.equals(Longitude_EW) || positionProperty.equals(Latitude_NS)) {
            return trim(string);
        }
        if (positionProperty.equals(Valid)) {
            string = trim(string);
            return "SPS".equals(string) || "DGPS".equals(string);
        }
        return null;
    }

    private PositionProperty determinePositionProperty(String headerField) {
        // TODO choose more elegant and robust way for mapping
        String property = trim(headerField.toLowerCase());

        List<Column> columns = getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if(column.getName().equals(property))
                return column;
        }
        if (property.equals("longitude")) {
            return Longitude;
        }
        if (property.equals("e/w")) {
            return Longitude_EW;
        }
        if (property.equals("latitude")) {
            return Latitude;
        }
        if (property.equals("n/s")) {
            return Latitude_NS;
        }
        if (property.equals("valid")) {
            return Valid;
        }
        return Ignore;
    }

    @SuppressWarnings("unchecked")
    public void write(R route, PrintWriter writer, int startIndex, int endIndex) throws IOException {
        List<Wgs84Position> positions = route.getPositions();
        writeHeader(writer);
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            writePosition(position, writer, i, i == startIndex);
        }
    }

    private void writeHeader(PrintWriter writer) {
        // TODO use PositionProperty
    }

    private void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        // TODO use PositionProperty
    }
}
