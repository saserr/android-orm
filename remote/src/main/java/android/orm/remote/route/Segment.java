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
import android.orm.sql.Readable;
import android.orm.sql.Select;
import android.orm.sql.Type;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.fragment.Condition;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Maybes.something;

public interface Segment {

    @NonNls
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

        @NonNls
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
        private final Condition.Builder<V> mCondition;
        @NonNls
        @NonNull
        private final String mWildcard;

        public Argument(@NonNull final Column<V> column,
                        @NonNull final Condition.Builder<V> condition) {
            super();

            mColumn = column;
            mName = column.getName();
            mProjection = column.getProjection();
            final Type.Primitive primitive = column.getType().getPrimitive();
            mWildcard = (primitive == Type.Primitive.Integer) ? "#" : "*";
            mCondition = condition;
        }

        @NonNls
        @NonNull
        public final String getName() {
            return mName;
        }

        @NonNull
        public final Select.Projection getProjection() {
            return mProjection;
        }

        @NonNls
        @NonNull
        @Override
        public final String toString() {
            return mWildcard;
        }

        @NonNls
        @NonNull
        public final String toString(@NonNull final V value) {
            return mColumn.toString(value);
        }

        @NonNull
        public final V fromString(@NonNull final String value) {
            return mColumn.fromString(value);
        }

        @NonNull
        public final Condition.Builder<V> getCondition() {
            return mCondition;
        }

        @NonNull
        public final Maybe<V> read(@NonNull final Readable input) {
            return mColumn.read(input);
        }

        public final void write(@NonNull final Value.Write.Operation operation,
                                @NonNull final V value,
                                @NonNull final Writable output) {
            mColumn.write(operation, something(value), output);
        }

        @NonNull
        public final Argument<V> not() {
            return new Argument<>(mColumn, mCondition.not());
        }

        @NonNull
        public final Argument<V> and(@NonNull final Condition condition) {
            return new Argument<>(mColumn, mCondition.and(condition));
        }

        @NonNull
        public final Argument<V> or(@NonNull final Condition condition) {
            return new Argument<>(mColumn, mCondition.or(condition));
        }
    }
}
