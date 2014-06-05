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

package android.orm.sql;

import android.database.SQLException;
import android.orm.sql.fragment.ConflictResolution;
import android.orm.sql.statement.Select;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.util.Maybes.something;

public class Column<V> extends Value.ReadWrite.Base<V> implements Fragment {

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Type<V> mType;
    @NonNull
    private final Select.Projection mProjection;
    @NonNull
    private final List<Constraint<V>> mConstraints;
    @NonNull
    private final Lazy<String> mSQL;
    @NonNull
    private final String mWildcard;
    private final boolean mNullable;
    private final boolean mUnique;
    private final boolean mRequired;

    private Column(@NonNls @NonNull final String name, @NonNull final Type<V> type) {
        this(name, type, Collections.<Constraint<V>>emptyList());
    }

    private Column(@NonNls @NonNull final String name,
                   @NonNull final Type<V> type,
                   @NonNull final List<Constraint<V>> constraints) {
        super();

        mName = name;
        mType = type;
        mProjection = Select.projection(name, Helper.escape(name));
        mConstraints = constraints;
        mSQL = sql(name, type, constraints);
        mWildcard = type.getWildcard();

        mNullable = !contains(Constraint.NotNull.class);
        mUnique = contains(Constraint.Unique.class);
        mRequired = contains(Constraint.Default.class) || mNullable;
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    public final Type<V> getType() {
        return mType;
    }

    @NonNull
    @Override
    public final Select.Projection getProjection() {
        return mProjection;
    }

    @NonNls
    @NonNull
    public final String getWildcard() {
        return mWildcard;
    }

    public final boolean isNullable() {
        return mNullable;
    }

    public final boolean isUnique() {
        return mUnique;
    }

    @NonNull
    public final Value.Read<V> as(@NonNls @NonNull final String name) {
        return Columns.readAs(name, this);
    }

    @NonNull
    @Override
    public final Maybe<V> read(@NonNull final Readable input) {
        return mType.read(input, mName);
    }

    @Override
    public final void write(@Value.Write.Operation final int operation,
                            @NonNull final Maybe<V> value,
                            @NonNull final Writable output) {
        final Maybe<V> processed = process(operation, value);

        if (processed.isSomething()) {
            final V v = processed.get();
            if (v == null) {
                output.putNull(mName);
            } else {
                mType.write(output, mName, v);
            }
        }

        if ((operation == Insert) && mRequired && !output.contains(mName)) {
            throw new SQLException("Required column " + mName + " is missing");
        }
    }

    @NonNull
    public final Column<V> asUnique() {
        return with(new Constraint.Unique<V>());
    }

    @NonNull
    public final Column<V> asUnique(@NonNull final ConflictResolution onConflict) {
        return with(new Constraint.Unique<V>(onConflict));
    }

    @NonNull
    public final Column<V> asNotNull() {
        return with(new Constraint.NotNull<V>());
    }

    @NonNull
    public final Column<V> asNotNull(@NonNull final ConflictResolution onConflict) {
        return with(new Constraint.NotNull<V>(onConflict));
    }

    @NonNull
    public final Column<V> withDefault(@Nullable final V value) {
        return with(new Constraint.Default<>(mType, value));
    }

    @NonNull
    public final Column<V> withDefault(@Nullable final Producer<V> producer) {
        return with(new Constraint.Default<>(producer));
    }

    @NonNull
    public final Column<V> with(@NonNull final Constraint<V> constraint) {
        final List<Constraint<V>> constraints = new ArrayList<>(mConstraints);
        constraints.add(constraint);
        return new Column<>(mName, mType, constraints);
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Column<?> other = (Column<?>) object;
            result = mName.equals(other.mName) &&
                    mType.equals(other.mType) &&
                    mConstraints.equals(other.mConstraints);
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return (31 * mName.hashCode()) + mType.hashCode();
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL.get();
    }

    @NonNull
    public final V fromString(@NonNull final String value) {
        return mType.fromString(value);
    }

    @NonNls
    @NonNull
    public final String toString(@NonNull final V value) {
        return mType.toString(value);
    }

    @NonNls
    @NonNull
    public final String escape(@NonNull final V value) {
        return mType.escape(value);
    }

    private <C extends Constraint<V>> boolean contains(@NonNull final Class<C> klass) {
        final List<Constraint<V>> constraints = mConstraints;
        final int size = constraints.size();
        boolean found = false;

        for (int i = 0; (i < size) && !found; i++) {
            found = klass.isInstance(constraints.get(i));
        }

        return found;
    }

    @NonNull
    private Maybe<V> process(@Value.Write.Operation final int operation,
                             @NonNull final Maybe<V> result) {
        Maybe<V> processed = result;

        for (final Constraint<V> constraint : mConstraints) {
            processed = constraint.beforeWrite(operation, processed);
        }

        return processed;
    }

    @NonNull
    public static <V> Column<V> column(@NonNls @NonNull final String name,
                                       @NonNull final Type<V> type) {
        return new Column<>(name, type);
    }

    public interface Constraint<V> {

        @NonNull
        Maybe<V> beforeWrite(@Value.Write.Operation final int operation,
                             @NonNull final Maybe<V> value);

        @NonNls
        @NonNull
        String toSQL(@NonNull final String column);

        class Unique<V> implements Constraint<V> {

            @NonNls
            @NonNull
            private final String mSQL;

            public Unique() {
                super();

                mSQL = "unique";
            }

            public Unique(@NonNull final ConflictResolution onConflict) {
                super();

                mSQL = "unique on conflict " + onConflict.toSQL();
            }

            @NonNull
            @Override
            public final Maybe<V> beforeWrite(@Value.Write.Operation final int operation,
                                              @NonNull final Maybe<V> value) {
                return value;
            }

            @NonNls
            @NonNull
            @Override
            public final String toSQL(@NonNull final String column) {
                return mSQL;
            }
        }

        class NotNull<V> implements Constraint<V> {

            @NonNls
            @NonNull
            private final String mSQL;

            public NotNull() {
                super();

                mSQL = "not null";
            }

            public NotNull(@NonNull final ConflictResolution onConflict) {
                super();

                mSQL = "not null on conflict " + onConflict.toSQL();
            }

            @NonNull
            @Override
            public final Maybe<V> beforeWrite(@Value.Write.Operation final int operation,
                                              @NonNull final Maybe<V> value) {
                if (value.isSomething() && (value.get() == null)) {
                    // TODO name
                    throw new SQLException("Value to be written cannot be null");
                }

                return value;
            }

            @NonNls
            @NonNull
            @Override
            public final String toSQL(@NonNull final String column) {
                return mSQL;
            }
        }

        class Default<V> implements Constraint<V> {

            @Nullable
            private final Producer<V> mProducer;
            @NonNls
            @NonNull
            private final String mSQL;

            public Default(@NonNull final Type<V> type,
                           @Nullable final V value) {
                super();

                if (value == null) {
                    mProducer = null;
                    mSQL = "default null";
                } else {
                    mProducer = Producers.singleton(value);
                    mSQL = "default " + type.escape(value);
                }
            }

            public Default(@Nullable final Producer<V> producer) {
                super();

                mProducer = producer;
                mSQL = "";
            }

            @NonNull
            @Override
            public final Maybe<V> beforeWrite(@Value.Write.Operation final int operation,
                                              @NonNull final Maybe<V> value) {
                return ((operation == Insert) && value.isNothing()) ?
                        something((mProducer == null) ? null : mProducer.produce()) :
                        value;
            }

            @NonNls
            @NonNull
            @Override
            public final String toSQL(@NonNull final String column) {
                return mSQL;
            }
        }
    }

    @NonNull
    private static <V> Lazy<String> sql(@NonNls @NonNull final String name,
                                        @NonNull final Type<V> type,
                                        @NonNull final Iterable<Constraint<V>> constraints) {
        return new Lazy.Volatile<String>() {
            @NonNls
            @NonNull
            @Override
            protected String produce() {
                @NonNls final StringBuilder result = new StringBuilder().append(Helper.escape(name));

                result.append(' ').append(type.toSQL());
                for (final Constraint<V> constraint : constraints) {
                    result.append(' ').append(constraint.toSQL(name));
                }

                return result.toString();
            }
        };
    }
}
