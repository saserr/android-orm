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

package android.orm.remote.route;

import android.orm.sql.Column;
import android.orm.sql.fragment.Condition;
import android.support.annotation.NonNull;

public final class Argument {

    @NonNull
    public static <V> Segment.Argument<V> isEqualTo(@NonNull final Column<V> column) {
        return new Segment.Argument<V>(validate(column)) {
            @NonNull
            @Override
            public Condition getCondition(@NonNull final V value) {
                return Condition.on(column).isEqualTo(value);
            }
        };
    }

    @NonNull
    public static <V> Segment.Argument<V> isNotEqualTo(@NonNull final Column<V> column) {
        return new Segment.Argument<V>(validate(column)) {
            @NonNull
            @Override
            public Condition getCondition(@NonNull final V value) {
                return Condition.on(column).isNotEqualTo(value);
            }
        };
    }

    @NonNull
    public static <V> Segment.Argument<V> isLessThan(@NonNull final Column<V> column) {
        return new Segment.Argument<V>(validate(column)) {
            @NonNull
            @Override
            public Condition getCondition(@NonNull final V value) {
                return Condition.on(column).isGreaterThan(value);
            }
        };
    }

    @NonNull
    public static <V> Segment.Argument<V> isLessOrEqualThan(@NonNull final Column<V> column) {
        return new Segment.Argument<V>(validate(column)) {
            @NonNull
            @Override
            public Condition getCondition(@NonNull final V value) {
                return Condition.on(column).isGreaterOrEqualThan(value);
            }
        };
    }

    @NonNull
    public static <V> Segment.Argument<V> isGreaterThan(@NonNull final Column<V> column) {
        return new Segment.Argument<V>(validate(column)) {
            @NonNull
            @Override
            public Condition getCondition(@NonNull final V value) {
                return Condition.on(column).isLessThan(value);
            }
        };
    }

    @NonNull
    public static <V> Segment.Argument<V> isGreaterOrEqualThan(@NonNull final Column<V> column) {
        return new Segment.Argument<V>(validate(column)) {
            @NonNull
            @Override
            public Condition getCondition(@NonNull final V value) {
                return Condition.on(column).isLessOrEqualThan(value);
            }
        };
    }

    @NonNull
    private static <V> Column<V> validate(@NonNull final Column<V> column) {
        if (column.isNullable()) {
            throw new IllegalArgumentException("Argument column should not be nullable");
        }

        return column;
    }

    private Argument() {
        super();
    }
}
