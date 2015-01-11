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

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.util.Lazy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.orm.sql.Helper.escape;
import static android.orm.sql.Readables.readable;
import static android.util.Log.INFO;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class Select {

    private static final String TAG = Select.class.getSimpleName();

    @NonNls
    @NonNull
    private final String mTable;
    @NonNull
    private final Condition mCondition;
    @Nullable
    private final Order mOrder;
    @Nullable
    private final Limit mLimit;
    @Nullable
    private final Offset mOffset;

    private Select(@NonNls @NonNull final String table,
                   @NonNull final Condition condition,
                   @Nullable final Order order,
                   @Nullable final Limit limit,
                   @Nullable final Offset offset) {
        super();

        mTable = table;
        mCondition = condition;
        mOrder = order;
        mLimit = limit;
        mOffset = offset;
    }

    @Nullable
    public final Readable execute(@NonNull final Projection projection,
                                  @NonNull final SQLiteDatabase database) {
        @org.jetbrains.annotations.Nullable final Cursor cursor;

        if (projection.isEmpty()) {
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was queried"); //NON-NLS
            }
            cursor = null;
        } else {
            final String sql = toSQL(projection, mTable, mCondition, mOrder, mLimit, mOffset);
            cursor = database.rawQuery(sql, null);
        }

        return (cursor == null) ? null : readable(cursor);
    }

    @NonNull
    public final Readable execute(@NonNull final SQLiteDatabase database) {
        return readable(database.rawQuery(toSQL(null, mTable, mCondition, mOrder, mLimit, mOffset), null));
    }

    @NonNull
    public static Builder select(@NonNls @NonNull final String table) {
        return new Builder(table);
    }

    @NonNull
    public static Projection projection(@NonNull final Column<?> column) {
        return projection(column.getName(), null);
    }

    @NonNull
    public static Projection projection(@NonNls @NonNull final String name,
                                        @NonNls @Nullable final String value) {
        return ((value != null) && escape(name).equals(value)) ?
                Projection.Base.create(Collections.<String, String>singletonMap(name, null)) :
                Projection.Base.create(singletonMap(name, value));
    }

    public static class Builder {

        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private Condition mCondition;
        @Nullable
        private Order mOrder;
        @Nullable
        private Limit mLimit;
        @Nullable
        private Offset mOffset;

        private Builder(@NonNls @NonNull final String table) {
            super();

            mTable = table;
            mCondition = Condition.None;
        }

        @NonNull
        public final Builder with(@NonNull final Condition condition) {
            mCondition = condition;
            return this;
        }

        @NonNull
        public final Builder with(@Nullable final Order order) {
            mOrder = order;
            return this;
        }

        @NonNull
        public final Builder with(@Nullable final Limit limit) {
            mLimit = limit;
            return this;
        }

        @NonNull
        public final Builder with(@Nullable final Offset offset) {
            mOffset = offset;
            return this;
        }

        @NonNull
        public final Select build() {
            return new Select(mTable, mCondition, mOrder, mLimit, mOffset);
        }
    }

    @NonNls
    @NonNull
    private static String toSQL(@Nullable final Projection projection,
                                @NonNls @NonNull final String table,
                                @NonNull final Condition condition,
                                @Nullable final Order order,
                                @Nullable final Limit limit,
                                @Nullable final Offset offset) {
        @NonNls final StringBuilder result = new StringBuilder();

        result.append("select ");
        if (projection == null) {
            result.append('*');
        } else {
            for (final String element : projection.asArray()) {
                result.append(element).append(", ");
            }
            final int length = result.length();
            result.delete(length - 2, length);
        }
        result.append('\n');

        result.append("from ").append(table);
        if (!condition.isEmpty()) {
            result.append('\n').append("where ").append(condition.toSQL());
        }
        if (order != null) {
            result.append('\n').append("order by ").append(order.toSQL());
        }
        if (limit != null) {
            result.append('\n').append("limit ").append(limit.toSQL());
        }
        if (offset != null) {
            result.append('\n').append("offset ").append(offset.toSQL());
        }

        return result.toString();
    }

    public interface Projection {

        boolean isEmpty();

        @NonNull
        String[] asArray();

        @NonNull
        Map<String, String> asMap();

        @NonNull
        Projection and(@NonNull final Projection other);

        @NonNull
        Projection without(@NonNull final Projection other);

        @NonNull
        Projection without(@NonNull final Set<String> names);

        Projection Nothing = new Projection() {

            @Override
            public boolean isEmpty() {
                return true;
            }

            @NonNull
            @Override
            public String[] asArray() {
                return new String[0];
            }

            @NonNull
            @Override
            public Map<String, String> asMap() {
                return emptyMap();
            }

            @NonNull
            @Override
            public Projection and(@NonNull final Projection other) {
                return other;
            }

            @NonNull
            @Override
            public Projection without(@NonNull final Projection other) {
                return this;
            }

            @NonNull
            @Override
            public Projection without(@NonNull final Set<String> names) {
                return this;
            }
        };

        abstract class Base implements Projection {

            @NonNull
            @Override
            public final Projection and(@NonNull final Projection other) {
                final Map<String, String> projection1 = asMap();
                final Map<String, String> projection2 = other.asMap();
                check(projection1, projection2);

                final Map<String, String> projection = new HashMap<>(projection1.size() + projection2.size());
                projection.putAll(projection1);
                projection.putAll(projection2);

                return create(projection);
            }

            @NonNull
            @Override
            public final Projection without(@NonNull final Projection other) {
                final Map<String, String> projection1 = asMap();
                final Map<String, String> projection2 = other.asMap();
                check(projection1, projection2);

                final Map<String, String> projection = new HashMap<>(projection1);
                for (final String name : projection2.keySet()) {
                    projection.remove(name);
                }

                return create(projection);
            }

            @NonNull
            @Override
            public final Projection without(@NonNull final Set<String> names) {
                final Map<String, String> projection1 = asMap();
                final Projection result;

                if (names.isEmpty()) {
                    result = this;
                } else {
                    final Map<String, String> projection = new HashMap<>(projection1);
                    for (final String name : projection1.keySet()) {
                        if (names.contains(name) || names.contains(escape(name))) {
                            projection.remove(name);
                        }
                    }
                    result = create(projection);
                }

                return result;
            }

            private static void check(@NonNull final Map<String, String> projection1,
                                      @NonNull final Map<String, String> projection2) {
                // get names that are in both projections
                final Collection<String> both = new HashSet<>(projection1.keySet());
                both.retainAll(projection2.keySet());

                for (final String name : both) {
                    // if values are not same in both projections throw error
                    @NonNls final String value1 = projection1.get(name);
                    @NonNls final String value2 = projection2.get(name);
                    if (!equals(value1, value2)) {
                        throw new SQLException("Projection has different values " + value1 + " and " + value2 + " with same name " + name);
                    }
                }
            }

            private static boolean equals(@NonNls @Nullable final String value1,
                                          @NonNls @Nullable final String value2) {
                return ((value1 == null) && (value2 == null)) ||
                        ((value1 != null) && value1.equals(value2));
            }

            @NonNull
            private static Projection create(@NonNull final Map<String, String> projection) {
                return projection.isEmpty() ?
                        Nothing :
                        new Base() {

                            private final Lazy<String[]> mArray = projectionAsArray(projection);

                            @Override
                            public boolean isEmpty() {
                                return false;
                            }

                            @NonNull
                            @Override
                            public String[] asArray() {
                                return mArray.get();
                            }

                            @NonNull
                            @Override
                            public Map<String, String> asMap() {
                                return projection;
                            }
                        };
            }

            @NonNull
            private static Lazy<String[]> projectionAsArray(@NonNull final Map<String, String> projection) {
                return new Lazy.Volatile<String[]>() {
                    @NonNull
                    @Override
                    protected String[] produce() {
                        final String[] result = new String[projection.size()];

                        int i = 0;
                        for (final Map.Entry<String, String> entry : projection.entrySet()) {
                            @NonNls final String name = escape(entry.getKey());
                            @NonNls final String value = entry.getValue();
                            result[i] = (value == null) ? name : (value + " as " + name); //NON-NLS
                            i++;
                        }

                        return result;
                    }
                };
            }
        }
    }
}
