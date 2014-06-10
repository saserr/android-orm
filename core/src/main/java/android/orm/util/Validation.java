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

package android.orm.util;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Maybes.something;
import static android.orm.util.Validations.valid;

public interface Validation<V> {

    @NonNull
    <T extends V> Result<T> validate(@NonNull final T v);

    @NonNull
    <T extends V> Result<Maybe<T>> validate(@NonNull final Maybe<T> m);

    @NonNull
    <T extends V> Validation<T> and(@NonNull final Validation<T> other);

    @NonNull
    Validation<V> name(@NonNls @NonNull final String name);

    class Exception extends RuntimeException {

        private static final long serialVersionUID = 53893318821367125L;

        public Exception(@NonNull final String error) {
            super(error);
        }

        public Exception(@NonNull final String error, @NonNull final Throwable cause) {
            super(error, cause);
        }
    }

    abstract class Base<V> implements Validation<V> {

        @NonNull
        @Override
        public final <T extends V> Validation<T> and(@NonNull final Validation<T> other) {
            return Validations.compose(this, other);
        }

        @NonNull
        @Override
        public final Validation<V> name(@NonNls @NonNull final String name) {
            return Validations.name(name, this);
        }
    }

    abstract class Value<V> extends Base<V> {
        @NonNull
        @Override
        public final <T extends V> Result<Maybe<T>> validate(@NonNull final Maybe<T> value) {
            final T t = value.getOrElse(null);
            final Result<Maybe<T>> result;

            if (t == null) {
                result = valid(value);
            } else {
                final Result<T> validated = validate(t);
                result = validated.isValid() ?
                        valid(something(validated.get())) :
                        Validations.<Maybe<T>>safeCast((Result.Invalid<T>) validated);
            }

            return result;
        }
    }

    interface Result<V> {

        boolean isValid();

        boolean isInvalid();

        @NonNull
        V get();

        @NonNull
        <T> Result<T> map(@NonNull final Function<? super V, ? extends T> function);

        @NonNull
        <T> Result<T> flatMap(@NonNull final Function<? super V, Result<T>> function);

        @NonNull
        <T extends V> Result<T> and(@NonNull final Result<T> second);

        @NonNull
        V or(@NonNull final V other);

        @NonNull
        <T extends V> Result<V> or(@NonNull final Result<T> other);

        abstract class Valid<V> implements Result<V> {

            @NonNull
            @Override
            public abstract <T> Valid<T> map(@NonNull final Function<? super V, ? extends T> function);

            @Override
            public final boolean isValid() {
                return true;
            }

            @Override
            public final boolean isInvalid() {
                return false;
            }
        }

        abstract class Invalid<V> implements Result<V> {

            @NonNls
            @NonNull
            public abstract String getError();

            @NonNull
            @Override
            public abstract <T> Invalid<T> map(@NonNull final Function<? super V, ? extends T> function);

            @NonNull
            @Override
            public abstract <T> Invalid<T> flatMap(@NonNull final Function<? super V, Result<T>> function);

            @NonNull
            @Override
            public abstract <T extends V> Invalid<T> and(@NonNull final Result<T> second);

            @Override
            public final boolean isValid() {
                return false;
            }

            @Override
            public final boolean isInvalid() {
                return true;
            }
        }
    }
}
