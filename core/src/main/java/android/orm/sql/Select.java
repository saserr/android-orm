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
import android.orm.sql.fragment.OrderType;
import android.orm.util.Function;
import android.orm.util.Lazy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.orm.sql.Helper.escape;
import static android.util.Log.INFO;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class Select {

    private static final String TAG = Select.class.getSimpleName();

    @NonNull
    private final Table<?> mTable;
    @NonNull
    private final Where mWhere;
    @Nullable
    private final Order mOrder;
    @Nullable
    private final Limit mLimit;
    @Nullable
    private final Offset mOffset;

    private <K> Select(@NonNull final Table<K> table,
                       @NonNull final Where where,
                       @Nullable final Order order,
                       @Nullable final Limit limit,
                       @Nullable final Offset offset) {
        super();

        mTable = table;
        mWhere = where;
        mOrder = order;
        mLimit = limit;
        mOffset = offset;
    }

    @Nullable
    public final Cursor execute(@NonNull final Projection projection,
                                @NonNull final SQLiteDatabase database) {
        @org.jetbrains.annotations.Nullable final Cursor cursor;

        if (projection.isEmpty()) {
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was queried"); //NON-NLS
            }
            cursor = null;
        } else {
            final String sql = toSQL(projection, mTable, mWhere, mOrder, mLimit, mOffset);
            cursor = database.rawQuery(sql, null);
        }

        return cursor;
    }

    @NonNull
    public static <K> Builder select(@NonNull final Table<K> table) {
        return new Builder(table);
    }

    @NonNull
    public static <V> Projection projection(@NonNull final Column<V> column) {
        return projection(column.getName(), escape(column.getName()));
    }

    @NonNull
    public static Projection projection(@NonNls @NonNull final String name,
                                        @NonNls @NonNull final String value) {
        return Projection.Base.create(singletonMap(name, value));
    }

    @NonNull
    public static <V> Where.Part<V> where(@NonNull final Column<V> column) {
        return new Where.Part<>(column);
    }

    @NonNull
    public static Where.TextPart whereText(@NonNull final Column<String> column) {
        return new Where.TextPart(column);
    }

    @NonNull
    public static <V> Order order(@NonNull final Column<V> column, @NonNull final OrderType type) {
        return new Order(escape(column.getName()) + ' ' + type.toSQL());
    }

    @NonNull
    public static Limit limit(final int amount) {
        return new Limit(String.valueOf(amount));
    }

    @NonNull
    public static Offset offset(final int amount) {
        return new Offset(String.valueOf(amount));
    }

    public static class Builder {

        @NonNull
        private final Table<?> mTable;
        @NonNull
        private Where mWhere;
        @Nullable
        private Order mOrder;
        @Nullable
        private Limit mLimit;
        @Nullable
        private Offset mOffset;

        public <K> Builder(@NonNull final Table<K> table) {
            super();

            mTable = table;
            mWhere = Where.None;
        }

        @NonNull
        public final Builder with(@NonNull final Where where) {
            mWhere = where;
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
            return new Select(mTable, mWhere, mOrder, mLimit, mOffset);
        }
    }

    public interface Projection {

        boolean isEmpty();

        @Nullable
        String[] asArray();

        @Nullable
        Map<String, String> asMap();

        @NonNull
        Projection and(@NonNull final Projection other);

        @NonNull
        Projection without(@NonNull final Projection other);

        @NonNull
        Projection without(@NonNull final Set<String> names);

        boolean isAny(@NonNull final Collection<String> columns);

        Projection All = new Projection() {

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Nullable
            @Override
            public String[] asArray() {
                return null;
            }

            @Nullable
            @Override
            public Map<String, String> asMap() {
                return null;
            }

            @NonNull
            @Override
            public Projection and(@NonNull final Projection other) {
                return this;
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

            @Override
            public boolean isAny(@NonNull final Collection<String> columns) {
                return true;
            }
        };

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

            @Override
            public boolean isAny(@NonNull final Collection<String> columns) {
                return false;
            }
        };

        abstract class Base implements Projection {

            @NonNull
            @Override
            public final Projection and(@NonNull final Projection other) {
                final Map<String, String> projection1 = asMap();
                final Map<String, String> projection2 = other.asMap();
                final Projection result;

                if ((projection1 == null) || (projection2 == null)) {
                    result = All;
                } else {
                    check(projection1, projection2);

                    final Map<String, String> projection = new HashMap<>(projection1.size() + projection2.size());
                    projection.putAll(projection1);
                    projection.putAll(projection2);
                    result = create(projection);
                }

                return result;
            }

            @NonNull
            @Override
            public final Projection without(@NonNull final Projection other) {
                final Map<String, String> projection1 = asMap();
                final Map<String, String> projection2 = other.asMap();
                final Projection result;

                if ((projection1 == null) || (projection2 == null)) {
                    result = All;
                } else {
                    check(projection1, projection2);

                    final Map<String, String> projection = new HashMap<>(projection1);
                    for (final String name : projection2.keySet()) {
                        projection.remove(name);
                    }
                    result = create(projection);
                }

                return result;
            }

            @NonNull
            @Override
            public final Projection without(@NonNull final Set<String> names) {
                final Map<String, String> projection1 = asMap();
                final Projection result;

                if (projection1 == null) {
                    result = All;
                } else if (names.isEmpty()) {
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
                    if (!value1.equals(value2)) {
                        throw new SQLException("Projection has different values " + value1 + " and " + value2 + " with same name " + name);
                    }
                }
            }

            @NonNull
            private static Projection create(@NonNull final Map<String, String> projection) {
                return projection.isEmpty() ?
                        Nothing :
                        new Base() {

                            private final Set<String> mNames = projection.keySet();
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

                            @Override
                            public boolean isAny(@NonNull final Collection<String> columns) {
                                final Collection<String> difference = new HashSet<>(mNames);
                                difference.retainAll(columns);
                                return !difference.isEmpty();
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
                            result[i] = name.equals(value) ? value : (value + " as " + name); //NON-NLS
                            i++;
                        }

                        return result;
                    }
                };
            }
        }
    }

    public static class Where implements Fragment {

        @NonNls
        private static final MessageFormat NOT = new MessageFormat("not ({0})");
        @NonNls
        private static final MessageFormat AND = new MessageFormat("({0}) and ({1})");
        @NonNls
        private static final MessageFormat OR = new MessageFormat("({0}) or ({1})");

        public static final Where None = new Where(null);

        @NonNls
        @Nullable
        private final String mSQL;

        public Where(@NonNls @Nullable final String selection) {
            super();

            mSQL = selection;
        }

        public final boolean isEmpty() {
            return mSQL == null;
        }

        @NonNull
        public final Where not() {
            return (mSQL == null) ? None : new Where(NOT.format(new String[]{mSQL}));
        }

        @NonNull
        public final Where and(@NonNull final Where other) {
            final Where result;

            if (mSQL == null) {
                result = (other.mSQL == null) ? None : other;
            } else {
                result = (other.mSQL == null) ?
                        this :
                        new Where(AND.format(new String[]{mSQL, other.mSQL}));
            }

            return result;
        }

        @NonNull
        public final Where or(@NonNull final Where other) {
            final Where result;

            if (mSQL == null) {
                result = (other.mSQL == null) ? None : other;
            } else {
                result = (other.mSQL == null) ?
                        this :
                        new Where(OR.format(new String[]{mSQL, other.mSQL}));
            }

            return result;
        }

        @NonNls
        @Nullable
        @Override
        public final String toSQL() {
            return mSQL;
        }

        public static class Part<V> {

            @NonNls
            private static final String COLUMN_NOT_NULLABLE = "Column should be nullable";

            @NonNull
            private final Column<V> mColumn;
            @NonNls
            @NonNull
            private final String mEscapedName;

            public Part(@NonNull final Column<V> column) {
                super();

                mColumn = column;
                mEscapedName = Helper.escape(column.getName());
            }

            @NonNull
            public final Where isNull() {
                if (!mColumn.isNullable()) {
                    throw new IllegalArgumentException(COLUMN_NOT_NULLABLE);
                }

                return new Where(mEscapedName + " is null");
            }

            @NonNull
            public final Where isNotNull() {
                if (!mColumn.isNullable()) {
                    throw new IllegalArgumentException(COLUMN_NOT_NULLABLE);
                }

                return new Where(mEscapedName + " is not null");
            }

            @NonNull
            public final Where isEqualTo(@NonNull final V value) {
                return new Where(mEscapedName + " = " + escape(value));
            }

            @NonNull
            public final Where isNotEqualTo(@NonNull final V value) {
                return new Where(mEscapedName + " <> " + escape(value));
            }

            @NonNull
            public final Where isLessThan(@NonNull final V value) {
                return new Where(mEscapedName + " < " + escape(value));
            }

            @NonNull
            public final Where isLessOrEqualThan(@NonNull final V value) {
                return new Where(mEscapedName + " <= " + escape(value));
            }

            @NonNull
            public final Where isGreaterThan(@NonNull final V value) {
                return new Where(mEscapedName + " > " + escape(value));
            }

            @NonNull
            public final Where isGreaterOrEqualThan(@NonNull final V value) {
                return new Where(mEscapedName + " >= " + escape(value));
            }

            @NonNull
            public final Where isBetween(@NonNull final V min, @NonNull final V max) {
                return new Where(mEscapedName + " between " + escape(min) + " and " + escape(max));
            }

            @NonNull
            public final Where isNotBetween(@NonNull final V min, @NonNull final V max) {
                return new Where(mEscapedName + " not between " + escape(min) + " and " + escape(max));
            }

            @NonNull
            protected String escape(@NonNull final V value) {
                return mColumn.escape(value);
            }
        }

        public static class TextPart extends Part<String> {

            @NonNls
            @NonNull
            private final String mEscapedName;

            public TextPart(@NonNull final Column<String> column) {
                super(column);

                mEscapedName = escape(column.getName());
            }

            @NonNull
            public final Where isLike(@NonNull final String pattern) {
                return new Where(mEscapedName + " like " + escape(pattern));
            }

            @NonNull
            public final Where isNotLike(@NonNull final String pattern) {
                return new Where(mEscapedName + " not like " + escape(pattern));
            }

            @NonNull
            public final Where isLikeGlob(@NonNull final String pattern) {
                return new Where(mEscapedName + " glob " + escape(pattern));
            }

            @NonNull
            public final Where isNotLikeGlob(@NonNull final String pattern) {
                return new Where(mEscapedName + " not glob " + escape(pattern));
            }

            @NonNull
            public final Where isLikeRegexp(@NonNull final String pattern) {
                return new Where(mEscapedName + " regexp " + escape(pattern));
            }

            @NonNull
            public final Where isNotLikeRegexp(@NonNull final String pattern) {
                return new Where(mEscapedName + " not regexp " + escape(pattern));
            }
        }

        public interface Builder<V> {

            @NonNull
            Builder<V> not();

            @NonNull
            Builder<V> and(@NonNull final Where other);

            @NonNull
            Builder<V> and(@NonNull final Builder<? super V> other);

            @NonNull
            Builder<V> or(@NonNull final Where other);

            @NonNull
            Builder<V> or(@NonNull final Builder<? super V> other);

            @NonNull
            Where build(@NonNull final V value);

            Builder<Object> None = builder(new Function<Object, Where>() {
                @NonNull
                @Override
                public Where invoke(@NonNull final Object argument) {
                    return Where.None;
                }
            });
        }

        @NonNull
        public static <V> Builder<V> builder(@NonNull final Function<V, Where> factory) {
            return new Builder<V>() {

                @NonNull
                @Override
                public Builder<V> not() {
                    return builder(new Function<V, Where>() {
                        @NonNull
                        @Override
                        public Where invoke(@NonNull final V value) {
                            return factory.invoke(value).not();
                        }
                    });
                }

                @NonNull
                @Override
                public Builder<V> and(@NonNull final Where other) {
                    return builder(new Function<V, Where>() {
                        @NonNull
                        @Override
                        public Where invoke(@NonNull final V value) {
                            return factory.invoke(value).and(other);
                        }
                    });
                }

                @NonNull
                @Override
                public Builder<V> and(@NonNull final Builder<? super V> other) {
                    return builder(new Function<V, Where>() {
                        @NonNull
                        @Override
                        public Where invoke(@NonNull final V value) {
                            return factory.invoke(value).and(other.build(value));
                        }
                    });
                }

                @NonNull
                @Override
                public Builder<V> or(@NonNull final Where other) {
                    return builder(new Function<V, Where>() {
                        @NonNull
                        @Override
                        public Where invoke(@NonNull final V value) {
                            return factory.invoke(value).or(other);
                        }
                    });
                }

                @NonNull
                @Override
                public Builder<V> or(@NonNull final Builder<? super V> other) {
                    return builder(new Function<V, Where>() {
                        @NonNull
                        @Override
                        public Where invoke(@NonNull final V value) {
                            return factory.invoke(value).or(other.build(value));
                        }
                    });
                }

                @NonNull
                @Override
                public Where build(@NonNull final V value) {
                    return factory.invoke(value);
                }
            };
        }
    }

    public static class Order implements Fragment {

        @NonNls
        @NonNull
        private final String mSQL;

        private Order(@NonNls @NonNull final String order) {
            super();

            mSQL = order;
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mSQL;
        }

        public final Order andThen(@NonNull final Order other) {
            return new Order(toSQL() + ", " + other.toSQL());
        }
    }

    public static class Limit implements Fragment {

        public static final Limit Single = new Limit(String.valueOf(1));

        @NonNls
        @NonNull
        private final String mSQL;

        private Limit(@NonNls @NonNull final String amount) {
            super();

            mSQL = amount;
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mSQL;
        }
    }

    public static class Offset implements Fragment {

        @NonNls
        @NonNull
        private final String mSQL;

        private Offset(@NonNls @NonNull final String amount) {
            super();

            mSQL = amount;
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mSQL;
        }
    }

    @NonNls
    @NonNull
    private static <K> String toSQL(@NonNull final Projection projection,
                                    @NonNull final Table<K> from,
                                    @NonNull final Where where,
                                    @Nullable final Order order,
                                    @Nullable final Limit limit,
                                    @Nullable final Offset offset) {
        @NonNls final StringBuilder result = new StringBuilder();

        result.append("select ");
        final Map<String, String> projectionMap = projection.asMap();
        if (projectionMap == null) {
            result.append('*');
        } else {
            for (final Map.Entry<String, String> entry : projectionMap.entrySet()) {
                @NonNls final String name = escape(entry.getKey());
                @NonNls final String value = entry.getValue();
                if (name.equals(value)) {
                    result.append(name);
                } else {
                    result.append(value).append(" as ").append(name); //NON-NLS
                }
                result.append(", ");
            }
            final int length = result.length();
            result.delete(length - 2, length);
        }
        result.append('\n');

        result.append("from ").append(escape(from.getName()));
        if (!where.isEmpty()) {
            result.append('\n').append("where ").append(where.toSQL());
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
}
