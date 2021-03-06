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
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedList;
import java.util.List;

public interface Validation<V> {

    boolean isValid(@NonNull final Maybe<? extends V> v);

    void validate(@NonNull final Maybe<? extends V> v);

    @NonNull
    <T extends V> Validation<T> and(@NonNull final Validation<T> other);

    @NonNull
    <T extends V> Validation<T> or(@NonNull final Validation<T> other);

    @NonNull
    Validation<V> name(@NonNls @NonNull final String name);

    @NonNull
    <M> Validation<M> on(@NonNull final Lens.Read<M, Maybe<V>> lens);

    @NonNull
    <T extends V> Validation<T> with(@NonNull final Callback<T> callback);

    @NonNull
    <T> Validation<T> map(@NonNull final Function<? super T, ? extends V> converter);

    @NonNull
    <T> Validation<T> flatMap(@NonNull final Function<? super T, Maybe<V>> converter);

    @NonNull
    <T> Validation<T> convert(@NonNull final Function<Maybe<T>, Maybe<V>> converter);

    class Failure extends RuntimeException {

        private static final long serialVersionUID = 53893318821367125L;

        public Failure(@NonNull final String error) {
            super(error);
        }

        public Failure(@NonNull final String error, @NonNull final Throwable cause) {
            super(error, cause);
        }
    }

    abstract class Base<V> implements Validation<V> {

        @NonNull
        @Override
        public final <T extends V> Validation<T> and(@NonNull final Validation<T> other) {
            return Validations.all(this, other);
        }

        @NonNull
        @Override
        public final <T extends V> Validation<T> or(@NonNull final Validation<T> other) {
            return Validations.any(this, other);
        }

        @NonNull
        @Override
        public final Validation<V> name(@NonNls @NonNull final String name) {
            return Validations.name(name, this);
        }

        @NonNull
        @Override
        public final <M> Validation<M> on(@NonNull final Lens.Read<M, Maybe<V>> lens) {
            return Validations.extend(this, lens);
        }

        @NonNull
        @Override
        public final <T extends V> Validation<T> with(@NonNull final Callback<T> callback) {
            return Validations.combine(this, callback);
        }

        @NonNull
        @Override
        public final <T> Validation<T> map(@NonNull final Function<? super T, ? extends V> converter) {
            return convert(Maybes.map(converter));
        }

        @NonNull
        @Override
        public final <T> Validation<T> flatMap(@NonNull final Function<? super T, Maybe<V>> converter) {
            return convert(Maybes.flatMap(converter));
        }

        @NonNull
        @Override
        public final <T> Validation<T> convert(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
            return Validations.convert(this, converter);
        }
    }

    abstract class Value<V> extends Base<V> {

        @NonNls
        @NonNull
        private final String mError;

        protected Value(@NonNls @NonNull final String error) {
            super();

            mError = error;
        }

        protected abstract boolean isValid(@Nullable final V v);

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends V> value) {
            return value.isSomething() && isValid(value.get());
        }

        @Override
        public final void validate(@NonNull final Maybe<? extends V> value) {
            if (!isValid(value)) {
                throw new Failure(mError);
            }
        }
    }

    interface Callback<V> {

        void onValid(@NonNull final Maybe<V> v);

        void onInvalid(@NonNull final Maybe<V> v);
    }

    class Builder<V> {

        private final List<Validation<? super V>> mValidations = new LinkedList<>();

        @NonNull
        public final Builder<V> with(@NonNull final Validation<? super V> validation) {
            mValidations.add(validation);
            return this;
        }

        @NonNull
        @SuppressWarnings("unchecked")
        public final Validation<V> build() {
            return Validations.all(mValidations.toArray(new Validation[mValidations.size()]));
        }
    }
}
