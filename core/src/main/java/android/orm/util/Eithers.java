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

public final class Eithers {

    private static final Object LEFT_LIFT = new Function.Base<Object, Either<Object, Object>>() {
        @NonNull
        @Override
        public Either<Object, Object> invoke(@NonNull final Object value) {
            return left(value);
        }
    };

    private static final Object RIGHT_LIFT = new Function.Base<Object, Either<Object, Object>>() {
        @NonNull
        @Override
        public Either<Object, Object> invoke(@NonNull final Object value) {
            return right(value);
        }
    };

    @NonNull
    public static <V, T> Either<V, T> left(@NonNull final V value) {
        return new Left<>(value);
    }

    @NonNull
    public static <V, T> Either<V, T> right(@NonNull final T value) {
        return new Right<>(value);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V, T> Function<V, Either<V, T>> leftLift() {
        return (Function<V, Either<V, T>>) LEFT_LIFT;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V, T> Function<T, Either<V, T>> rightLift() {
        return (Function<T, Either<V, T>>) RIGHT_LIFT;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V, T> Either<V, T> safeCast(@NonNull final Either<? extends V, ? extends T> either) {
        return (Either<V, T>) either;
    }

    private static class Left<V, T> implements Either<V, T> {

        @NonNull
        private final V mValue;

        private Left(@NonNull final V value) {
            super();

            mValue = value;
        }

        @Override
        public final boolean isLeft() {
            return true;
        }

        @Override
        public final boolean isRight() {
            return false;
        }

        @NonNull
        @Override
        public final V getLeft() {
            return mValue;
        }

        @NonNull
        @Override
        public final T getRight() {
            throw new UnsupportedOperationException("Either is left");
        }

        @Override
        public final boolean equals(@Nullable final Object object) {
            boolean result = this == object;

            if (!result && (object != null) && (object instanceof Either)) {
                final Either<?, ?> other = (Either<?, ?>) object;
                if (other.isLeft()) {
                    final Object value = other.getLeft();
                    result = mValue.equals(value);
                }
            }

            return result;
        }

        @Override
        public final int hashCode() {
            return mValue.hashCode();
        }
    }

    private static class Right<V, T> implements Either<V, T> {

        @NonNull
        private final T mValue;

        private Right(@NonNull final T value) {
            super();

            mValue = value;
        }

        @Override
        public final boolean isLeft() {
            return false;
        }

        @Override
        public final boolean isRight() {
            return true;
        }

        @NonNull
        @Override
        public final V getLeft() {
            throw new UnsupportedOperationException("Either is right");
        }

        @NonNull
        @Override
        public final T getRight() {
            return mValue;
        }

        @Override
        public final boolean equals(@Nullable final Object object) {
            boolean result = this == object;

            if (!result && (object != null) && (object instanceof Either)) {
                final Either<?, ?> other = (Either<?, ?>) object;
                if (other.isRight()) {
                    final Object value = other.getRight();
                    result = mValue.equals(value);
                }
            }

            return result;
        }

        @Override
        public final int hashCode() {
            return mValue.hashCode();
        }
    }

    private Eithers() {
        super();
    }
}
