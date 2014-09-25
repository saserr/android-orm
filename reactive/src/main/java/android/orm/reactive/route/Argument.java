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

package android.orm.reactive.route;

import android.orm.sql.Column;
import android.orm.sql.fragment.Where;
import android.orm.util.Function;
import android.support.annotation.NonNull;

import static android.orm.sql.Helper.escape;

public final class Argument {

    @NonNull
    public static <V> Segment.Argument<V> isEqualTo(@NonNull final Column<V> column) {
        return segment(validate(column), " = ");
    }

    @NonNull
    public static <V> Segment.Argument<V> isNotEqualTo(@NonNull final Column<V> column) {
        return segment(validate(column), " <> ");
    }

    @NonNull
    public static <V> Segment.Argument<V> isLessThan(@NonNull final Column<V> column) {
        return segment(validate(column), " > ");
    }

    @NonNull
    public static <V> Segment.Argument<V> isLessOrEqualThan(@NonNull final Column<V> column) {
        return segment(validate(column), " >= ");
    }

    @NonNull
    public static <V> Segment.Argument<V> isGreaterThan(@NonNull final Column<V> column) {
        return segment(validate(column), " < ");
    }

    @NonNull
    public static <V> Segment.Argument<V> isGreaterOrEqualThan(@NonNull final Column<V> column) {
        return segment(validate(column), " <= ");
    }

    @NonNull
    private static <V> Column<V> validate(@NonNull final Column<V> column) {
        if (column.isNullable()) {
            throw new IllegalArgumentException("Argument column should not be nullable");
        }

        return column;
    }

    @NonNull
    private static <V> Segment.Argument<V> segment(@NonNull final Column<V> column,
                                                   @NonNull final String operation) {
        return new Segment.Argument<>(column, Where.builder(new Function<V, Where>() {
            @NonNull
            @Override
            public Where invoke(@NonNull final V value) {
                return new Where(escape(column.getName()) + operation + column.escape(value));
            }
        }));
    }

    private Argument() {
        super();
    }
}
