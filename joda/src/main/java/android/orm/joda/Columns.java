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

import android.orm.sql.Column;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import static android.orm.joda.Types.Date;
import static android.orm.joda.Types.Time;
import static android.orm.joda.Types.Timestamp;
import static android.orm.sql.Column.column;

public final class Columns {

    @NonNull
    public static Column<LocalTime> time(@NonNls @NonNull final String name) {
        return column(name, Time);
    }

    @NonNull
    public static Column<LocalDate> date(@NonNls @NonNull final String name) {
        return column(name, Date);
    }

    @NonNull
    public static Column<DateTime> timestamp(@NonNls @NonNull final String name) {
        return column(name, Timestamp);
    }

    private Columns() {
        super();
    }
}
