/*
 * Copyright 2014 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.orm.joda;

import android.orm.sql.Type;
import android.orm.util.Converter;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import static android.orm.sql.Types.Integer;
import static android.orm.sql.Types.Text;

public final class Types {

    public static final Type<LocalTime> Time = Integer.map(
            new Converter<LocalTime, Long>() {

                @NonNull
                @Override
                public Long from(@NonNull final LocalTime time) {
                    return (long) time.getMillisOfDay();
                }

                @NonNull
                @Override
                public LocalTime to(@NonNull final Long value) {
                    return LocalTime.fromMillisOfDay(value.intValue());
                }
            }
    );

    public static final Type<LocalDate> Date = Text.map(
            new Converter<LocalDate, String>() {

                private final DateTimeFormatter mFormatter = ISODateTimeFormat.date();

                @NonNull
                @Override
                public String from(@NonNull final LocalDate date) {
                    return mFormatter.print(date);
                }

                @NonNull
                @Override
                public LocalDate to(@NonNull final String value) {
                    return mFormatter.parseLocalDate(value);
                }
            }
    );

    public static final Type<DateTime> Timestamp = Integer.map(
            new Converter<DateTime, Long>() {

                @NonNull
                @Override
                public Long from(@NonNull final DateTime date) {
                    return date.getMillis();
                }

                @NonNull
                @Override
                public DateTime to(@NonNull final Long value) {
                    return new DateTime(value.longValue());
                }
            }
    );

    private Types() {
        super();
    }
}
