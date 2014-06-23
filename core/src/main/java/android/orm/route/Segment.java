/*
 * Copyright 2013 the original author or authors
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

package android.orm.route;

import android.orm.sql.Column;
import android.orm.sql.Readable;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.statement.Select;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Maybes.something;

public interface Segment {

    @NonNull
    @Override
    String toString();

    class Literal implements Segment {

        @NonNull
        private final String mValue;

        Literal(@NonNls @NonNull final String value) {
            super();

            mValue = value;
        }

        @NonNull
        @Override
        public final String toString() {
            return mValue;
        }
    }

    class Argument<V> implements Segment {

        @NonNull
        private final Column<V> mColumn;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Select.Projection mProjection;
        @NonNull
        private final Select.Where.Builder<V> mWhere;
        @NonNull
        private final String mWildcard;

        public Argument(@NonNull final Column<V> column,
                        @NonNull final Select.Where.Builder<V> where) {
            super();

            mColumn = column;
            mName = column.getName();
            mProjection = column.getProjection();
            mWildcard = column.getWildcard();
            mWhere = where;
        }

        @NonNull
        public final String getName() {
            return mName;
        }

        @NonNull
        public final Select.Projection getProjection() {
            return mProjection;
        }

        @NonNull
        @Override
        public final String toString() {
            return mWildcard;
        }

        @NonNull
        public final String toString(@NonNull final V value) {
            return mColumn.toString(value);
        }

        @NonNull
        public final V fromString(@NonNull final String value) {
            return mColumn.fromString(value);
        }

        @NonNull
        public final Select.Where.Builder<V> getWhere() {
            return mWhere;
        }

        @NonNull
        public final V read(@NonNull final Readable input) {
            final V result = mColumn.read(input).getOrElse(null);
            if (result == null) {
                throw new IllegalArgumentException("Missing argument " + mName);
            }
            return result;
        }

        public final void write(@NonNull final Value.Write.Operation operation,
                                @NonNull final V value,
                                @NonNull final Writable output) {
            mColumn.write(operation, something(value), output);
        }

        @NonNull
        public final Argument<V> not() {
            return new Argument<>(mColumn, mWhere.not());
        }

        @NonNull
        public final Argument<V> and(@NonNull final Select.Where where) {
            return new Argument<>(mColumn, mWhere.and(where));
        }

        @NonNull
        public final Argument<V> or(@NonNull final Select.Where where) {
            return new Argument<>(mColumn, mWhere.or(where));
        }
    }
}
