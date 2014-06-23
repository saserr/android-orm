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

package android.orm.sql;

import android.orm.util.Function;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.math.BigDecimal;

import static android.orm.sql.Helper.escape;
import static android.orm.sql.Types.Decimal;

public final class AggregateFunctions {

    @NonNls
    private static final String COUNT = "count";
    @NonNls
    private static final String MIN = "min";
    @NonNls
    private static final String MAX = "max";
    @NonNls
    private static final String SUM = "sum";
    @NonNls
    private static final String TOTAL = "total";
    @NonNls
    private static final String AVERAGE = "avg";

    public static final AggregateFunction.Builder<Long> CountRows = new AggregateFunction.Builder<Long>() {
        @NonNull
        @Override
        public AggregateFunction<Long> as(@NonNls @NonNull final String name) {
            return new OnNumber.BaseFunction(name, Select.projection(name, COUNT + "(*)"));
        }
    };

    @NonNull
    public static <V> AggregateFunction.Builder<Long> count(@NonNull final Column<V> column) {
        return new OnNumber.Builder<>(COUNT, column);
    }

    public static final class OnNumber {

        @NonNull
        public static AggregateFunction.Builder<Long> min(@NonNull final Column<Long> column) {
            return new Builder<>(MIN, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Long> max(@NonNull final Column<Long> column) {
            return new Builder<>(MAX, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Long> sum(@NonNull final Column<Long> column) {
            return new Builder<>(SUM, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Long> total(@NonNull final Column<Long> column) {
            return new Builder<>(TOTAL, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Double> average(@NonNull final Column<Long> column) {
            return new OnReal.Builder<>(AVERAGE, column);
        }

        private static class Builder<V> implements AggregateFunction.Builder<Long> {

            @NonNls
            @NonNull
            private final String mFunction;
            @NonNull
            private final Column<V> mColumn;

            private Builder(@NonNls @NonNull final String function,
                            @NonNull final Column<V> column) {
                super();

                mFunction = function;
                mColumn = column;
            }

            @NonNull
            @Override
            public final AggregateFunction<Long> as(@NonNls @NonNull final String name) {
                return new BaseFunction(mFunction, mColumn, name);
            }
        }

        private static class BaseFunction extends AggregateFunctions.BaseFunction<Long> {

            @NonNls
            @NonNull
            private final String mName;

            private <V> BaseFunction(@NonNls @NonNull final String function,
                                     @NonNull final Column<V> column,
                                     @NonNls @NonNull final String name) {
                super(function, column, name);

                mName = name;
            }

            private BaseFunction(@NonNls @NonNull final String name,
                                 @NonNull final Select.Projection projection) {
                super(name, projection);

                mName = name;
            }

            @NonNull
            @Override
            public final Maybe<Long> read(@NonNull final Readable input) {
                return input.getAsLong(mName);
            }
        }

        private OnNumber() {
            super();
        }
    }

    public static final class OnReal {

        @NonNull
        public static AggregateFunction.Builder<Double> min(@NonNull final Column<Double> column) {
            return new Builder<>(MIN, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Double> max(@NonNull final Column<Double> column) {
            return new Builder<>(MAX, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Double> sum(@NonNull final Column<Double> column) {
            return new Builder<>(SUM, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Double> total(@NonNull final Column<Double> column) {
            return new Builder<>(TOTAL, column);
        }

        @NonNull
        public static AggregateFunction.Builder<Double> average(@NonNull final Column<Double> column) {
            return new Builder<>(AVERAGE, column);
        }

        private static class Builder<V> implements AggregateFunction.Builder<Double> {

            @NonNls
            @NonNull
            private final String mFunction;
            @NonNull
            private final Column<V> mColumn;

            private Builder(@NonNls @NonNull final String function,
                            @NonNull final Column<V> column) {
                super();

                mFunction = function;
                mColumn = column;
            }

            @NonNull
            @Override
            public final AggregateFunction<Double> as(@NonNls @NonNull final String name) {
                return new BaseFunction(mFunction, mColumn, name);
            }
        }

        private static class BaseFunction extends AggregateFunctions.BaseFunction<Double> {

            @NonNls
            @NonNull
            private final String mName;

            private <V> BaseFunction(@NonNls @NonNull final String function,
                                     @NonNull final Column<V> column,
                                     @NonNls @NonNull final String name) {
                super(function, column, name);

                mName = name;
            }

            @NonNull
            @Override
            public final Maybe<Double> read(@NonNull final Readable input) {
                return input.getAsDouble(mName);
            }
        }

        private OnReal() {
            super();
        }
    }

    public static final class OnDecimal {

        @NonNull
        public static AggregateFunction.Builder<BigDecimal> min(@NonNull final Column<BigDecimal> column) {
            return new Builder(MIN, column);
        }

        @NonNull
        public static AggregateFunction.Builder<BigDecimal> max(@NonNull final Column<BigDecimal> column) {
            return new Builder(MAX, column);
        }

        @NonNull
        public static AggregateFunction.Builder<BigDecimal> sum(@NonNull final Column<BigDecimal> column) {
            return new Builder(SUM, column);
        }

        @NonNull
        public static AggregateFunction.Builder<BigDecimal> total(@NonNull final Column<BigDecimal> column) {
            return new Builder(TOTAL, column);
        }

        @NonNull
        public static AggregateFunction.Builder<BigDecimal> average(@NonNull final Column<BigDecimal> column) {
            return new Builder(AVERAGE, column);
        }

        private static class Builder implements AggregateFunction.Builder<BigDecimal> {

            @NonNls
            @NonNull
            private final String mFunction;
            @NonNull
            private final Column<BigDecimal> mColumn;

            private Builder(@NonNls @NonNull final String function,
                            @NonNull final Column<BigDecimal> column) {
                super();

                mFunction = function;
                mColumn = column;
            }

            @NonNull
            @Override
            public final AggregateFunction<BigDecimal> as(@NonNls @NonNull final String name) {
                return new BaseFunction(mFunction, mColumn, name);
            }
        }

        private static class BaseFunction extends AggregateFunctions.BaseFunction<BigDecimal> {

            @NonNls
            @NonNull
            private final String mName;

            private <V> BaseFunction(@NonNls @NonNull final String function,
                                     @NonNull final Column<V> column,
                                     @NonNls @NonNull final String name) {
                super(function, column, name);

                mName = name;
            }

            @NonNull
            @Override
            public final Maybe<BigDecimal> read(@NonNull final Readable input) {
                return Decimal.read(input, mName);
            }
        }

        private OnDecimal() {
            super();
        }
    }

    @NonNull
    public static <V, T> AggregateFunction<Pair<V, T>> compose(@NonNull final AggregateFunction<V> first,
                                                               @NonNull final AggregateFunction<T> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    public static <V, T> AggregateFunction<T> convert(@NonNull final AggregateFunction<V> function,
                                                      @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new Conversion<>(function, converter);
    }

    private abstract static class BaseFunction<V> extends AggregateFunction.Base<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Select.Projection mProjection;

        protected <T> BaseFunction(@NonNls @NonNull final String function,
                                   @NonNull final Column<T> column,
                                   @NonNls @NonNull final String name) {
            this(name, Select.projection(name, function + '(' + escape(column.getName()) + ')'));
        }

        protected BaseFunction(@NonNls @NonNull final String name,
                               @NonNull final Select.Projection projection) {
            super();

            mName = name;
            mProjection = projection;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mProjection;
        }
    }

    private static class Composition<V, T> extends AggregateFunction.Base<Pair<V, T>> {

        @NonNull
        private final AggregateFunction<V> mFirst;
        @NonNull
        private final AggregateFunction<T> mSecond;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Select.Projection mProjection;

        private Composition(@NonNull final AggregateFunction<V> first,
                            @NonNull final AggregateFunction<T> second) {
            super();

            mFirst = first;
            mSecond = second;
            mName = '(' + mFirst.getName() + ", " + mSecond.getName() + ')';
            mProjection = mFirst.getProjection().and(mSecond.getProjection());
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mProjection;
        }

        @NonNull
        @Override
        public final Maybe<Pair<V, T>> read(@NonNull final Readable input) {
            return mFirst.read(input).and(mSecond.read(input));
        }
    }

    private static class Conversion<V, T> extends AggregateFunction.Base<T> {

        @NonNull
        private final AggregateFunction<V> mFunction;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mConverter;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Select.Projection mProjection;

        private Conversion(@NonNull final AggregateFunction<V> function,
                           @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            super();

            mFunction = function;
            mConverter = converter;
            mName = function.getName();
            mProjection = mFunction.getProjection();
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mProjection;
        }

        @NonNull
        @Override
        public final Maybe<T> read(@NonNull final Readable input) {
            return mConverter.invoke(mFunction.read(input));
        }
    }

    private AggregateFunctions() {
        super();
    }
}
