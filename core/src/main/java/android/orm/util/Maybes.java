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

package android.orm.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

public final class Maybes {

    private static final Something<Object> Null = new Something<>(null);
    private static final Nothing<Object> Nothing = new Nothing<>();

    private static final Object LiftValue = new Function.Base<Object, Maybe<Object>>() {
        @NonNull
        @Override
        public Maybe<Object> invoke(@NonNull final Object value) {
            return something(value);
        }
    };

    private static final Object LiftPair = new Function.Base<Pair<Maybe<Object>, Maybe<Object>>, Maybe<Pair<Object, Object>>>() {
        @NonNull
        @Override
        public Maybe<Pair<Object, Object>> invoke(@NonNull final Pair<Maybe<Object>, Maybe<Object>> pair) {
            final Maybe<Pair<Object, Object>> result;

            if (pair.first == null) {
                if (pair.second == null) {
                    result = something(null);
                } else {
                    result = pair.second.isSomething() ?
                            something(Pair.create(null, pair.second.get())) :
                            Maybes.<Pair<Object, Object>>nothing();
                }
            } else {
                if (pair.second == null) {
                    result = pair.first.isSomething() ?
                            something(Pair.create(pair.first.get(), null)) :
                            Maybes.<Pair<Object, Object>>nothing();
                } else {
                    result = pair.first.and(pair.second);
                }
            }

            return result;
        }
    };

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Maybe<V> something(@Nullable final V value) {
        return (value == null) ? (Something<V>) Null : new Something<>(value);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Maybe<V> nothing() {
        return (Maybe<V>) Nothing;
    }

    @NonNull
    public static <V, T> Function<Maybe<V>, Maybe<T>> map(@NonNull final Function<? super V, ? extends T> function) {
        return new Map<>(function);
    }

    @NonNull
    public static <V, T> Function<Maybe<V>, Maybe<T>> flatMap(@NonNull final Function<? super V, Maybe<T>> function) {
        return new FlatMap<>(function);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Function<V, Maybe<V>> liftValue() {
        return (Function<V, Maybe<V>>) LiftValue;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V, T> Function<Pair<Maybe<V>, Maybe<T>>, Maybe<Pair<V, T>>> liftPair() {
        return (Function<Pair<Maybe<V>, Maybe<T>>, Maybe<Pair<V, T>>>) LiftPair;
    }

    @NonNull
    public static <V, T> Converter<Maybe<V>, Maybe<T>> lift(@NonNull final Converter<V, T> converter) {
        return new LiftedConverter<>(converter);
    }

    @NonNull
    public static <M, V> Lens.Read<M, Maybe<V>> lift(@NonNull final Lens.Read<M, ? extends V> lens) {
        return lens.mapTo(Maybes.<V>liftValue());
    }

    @NonNull
    public static <M, V> Lens.Write<M, Maybe<V>> lift(@NonNull final Lens.Write<M, ? super V> lens) {
        return new LiftedWriteLens<>(lens);
    }

    @NonNull
    public static <M, V> Lens.ReadWrite<M, Maybe<V>> lift(@NonNull final Lens.ReadWrite<M, V> lens) {
        return Lenses.combine(lift((Lens.Read<M, V>) lens), lift((Lens.Write<M, V>) lens));
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Maybe<V> safeCast(@NonNull final Maybe<? extends V> value) {
        return (Maybe<V>) value;
    }

    private static class Something<V> implements Maybe<V> {

        @Nullable
        private final V mValue;

        private Something(@Nullable final V value) {
            super();

            mValue = value;
        }

        @Override
        public final boolean isSomething() {
            return true;
        }

        @Override
        public final boolean isNothing() {
            return false;
        }

        @Nullable
        @Override
        public final V get() {
            return mValue;
        }

        @NonNull
        @Override
        public final <T> Maybe<Pair<V, T>> and(@NonNull final Maybe<? extends T> other) {
            final Maybe<Pair<V, T>> result;

            if (other.isSomething()) {
                final T second = other.get();
                result = something(
                        ((mValue == null) && (second == null)) ? null : Pair.create(mValue, second)
                );
            } else {
                result = nothing();
            }

            return result;
        }

        @Nullable
        @Override
        public final <T extends V> V getOrElse(@Nullable final T other) {
            return mValue;
        }

        @NonNull
        @Override
        public final <T extends V> Maybe<V> orElse(@NonNull final Maybe<T> other) {
            return this;
        }

        @NonNull
        @Override
        public final <T> Maybe<T> map(@NonNull final Function<? super V, ? extends T> function) {
            return something((mValue == null) ? null : function.invoke(mValue));
        }

        @NonNull
        @Override
        public final <T> Maybe<T> flatMap(@NonNull final Function<? super V, Maybe<T>> function) {
            return (mValue == null) ? Maybes.<T>something(null) : function.invoke(mValue);
        }

        @Override
        public final boolean equals(@Nullable final Object object) {
            boolean result = this == object;

            if (!result && (object != null) && (object instanceof Maybe)) {
                final Maybe<?> other = (Maybe<?>) object;
                if (other.isSomething()) {
                    final Object value = other.get();
                    result = (mValue == null) ?
                            (value == null) :
                            ((value != null) && mValue.equals(value));
                }
            }

            return result;
        }

        @Override
        public final int hashCode() {
            return (mValue == null) ? 0 : mValue.hashCode();
        }
    }

    private static class Nothing<V> implements Maybe<V> {

        @Override
        public final boolean isSomething() {
            return false;
        }

        @Override
        public final boolean isNothing() {
            return true;
        }

        @Nullable
        @Override
        public final V get() {
            throw new UnsupportedOperationException("Cannot get value on nothing");
        }

        @NonNull
        @Override
        public final <T> Maybe<Pair<V, T>> and(@NonNull final Maybe<? extends T> other) {
            return nothing();
        }

        @Nullable
        @Override
        public final <T extends V> V getOrElse(@Nullable final T other) {
            return other;
        }

        @NonNull
        @Override
        public final <T extends V> Maybe<V> orElse(@NonNull final Maybe<T> other) {
            return Maybes.<V>safeCast(other);
        }

        @NonNull
        @Override
        public final <T> Maybe<T> map(@NonNull final Function<? super V, ? extends T> function) {
            return nothing();
        }

        @NonNull
        @Override
        public final <T> Maybe<T> flatMap(@NonNull final Function<? super V, Maybe<T>> function) {
            return nothing();
        }

        @Override
        public final boolean equals(@Nullable final Object object) {
            boolean result = this == object;

            if (!result && (object != null) && (object instanceof Maybe)) {
                result = ((Maybe<?>) object).isNothing();
            }

            return result;
        }

        @Override
        public final int hashCode() {
            return 0;
        }

        private Nothing() {
            super();
        }
    }

    private static class Map<V, T> extends Function.Base<Maybe<V>, Maybe<T>> {

        @NonNull
        private final Function<? super V, ? extends T> mFunction;

        private Map(@NonNull final Function<? super V, ? extends T> function) {
            super();

            mFunction = function;
        }

        @NonNull
        @Override
        public final Maybe<T> invoke(@NonNull final Maybe<V> value) {
            return safeCast(value.map(mFunction));
        }
    }

    private static class FlatMap<V, T> extends Function.Base<Maybe<V>, Maybe<T>> {

        @NonNull
        private final Function<? super V, Maybe<T>> mFunction;

        private FlatMap(@NonNull final Function<? super V, Maybe<T>> function) {
            super();

            mFunction = function;
        }

        @NonNull
        @Override
        public final Maybe<T> invoke(@NonNull final Maybe<V> value) {
            return value.flatMap(mFunction);
        }
    }

    private static class LiftedConverter<V, T> implements Converter<Maybe<V>, Maybe<T>> {

        private final Function<V, Maybe<T>> mFrom;

        private final Function<T, Maybe<V>> mTo;

        private LiftedConverter(@NonNull final Converter<V, T> converter) {
            super();

            mFrom = new Function.Base<V, Maybe<T>>() {
                @NonNull
                @Override
                public Maybe<T> invoke(@NonNull final V v) {
                    return something(converter.from(v));
                }
            };

            mTo = new Function.Base<T, Maybe<V>>() {
                @NonNull
                @Override
                public Maybe<V> invoke(@NonNull final T t) {
                    return something(converter.to(t));
                }
            };
        }

        @NonNull
        @Override
        public final Maybe<T> from(@NonNull final Maybe<V> result) {
            return result.flatMap(mFrom);
        }

        @NonNull
        @Override
        public final Maybe<V> to(@NonNull final Maybe<T> result) {
            return result.flatMap(mTo);
        }
    }

    private static class LiftedWriteLens<V, T> extends Lens.Write.Base<V, Maybe<T>> {

        @NonNull
        private final Lens.Write<V, ? super T> mLens;

        private LiftedWriteLens(@NonNull final Lens.Write<V, ? super T> lens) {
            super();

            mLens = lens;
        }

        @Override
        public final void set(@NonNull final V model, @Nullable final Maybe<T> value) {
            if (value == null) {
                mLens.set(model, null);
            } else if (value.isSomething()) {
                mLens.set(model, value.get());
            }
        }
    }

    private Maybes() {
        super();
    }
}
