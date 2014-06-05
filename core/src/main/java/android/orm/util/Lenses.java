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

public final class Lenses {

    @NonNull
    public static <M, V> Lens.Read<M, V> check(@NonNull final Lens.Read<M, V> lens,
                                               @NonNull final Validation<? super V> validation) {
        return new Check<>(lens, validation);
    }

    @NonNull
    public static <M, V> Lens.ReadWrite<M, V> check(@NonNull final Lens.ReadWrite<M, V> lens,
                                                    @NonNull final Validation<? super V> validation) {
        return combine(new Check<>(lens, validation), lens);
    }

    @NonNull
    public static <M, V, T> Lens.Read<M, T> convert(@NonNull final Lens.Read<M, V> lens,
                                                    @NonNull final Function<? super V, ? extends T> converter) {
        return new ReadConversion<>(lens, converter);
    }

    @NonNull
    public static <M, V, T> Lens.Write<M, T> convert(@NonNull final Lens.Write<M, V> lens,
                                                     @NonNull final Function<? super T, ? extends V> converter) {
        return new WriteConversion<>(lens, converter);
    }

    @NonNull
    public static <M, V> Lens.ReadWrite<M, V> combine(@NonNull final Lens.Read<M, ? extends V> read,
                                                      @NonNull final Lens.Write<M, ? super V> write) {
        return new Combine<>(read, write);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <M, V> Lens.Read<M, V> safeCast(@NonNull final Lens.Read<M, ? extends V> lens) {
        return (Lens.Read<M, V>) lens;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <M, V> Lens.Write<M, V> safeCast(@NonNull final Lens.Write<M, ? super V> lens) {
        return (Lens.Write<M, V>) lens;
    }

    private static class Check<M, V> extends Lens.Read.Base<M, V> {

        @NonNull
        private final Lens.Read<M, V> mLens;
        @NonNull
        private final Validation<? super V> mValidation;

        private Check(@NonNull final Lens.Read<M, V> lens,
                      @NonNull final Validation<? super V> validation) {
            super();

            mLens = lens;
            mValidation = validation;
        }

        @Nullable
        @Override
        public final V get(@NonNull final M model) {
            final V value = mLens.get(model);
            return (value == null) ? null : mValidation.validate(value).get();
        }
    }

    private static class ReadConversion<M, V, T> extends Lens.Read.Base<M, T> {

        @NonNull
        private final Lens.Read<M, V> mLens;
        @NonNull
        private final Function<? super V, ? extends T> mConverter;

        private ReadConversion(@NonNull final Lens.Read<M, V> lens,
                               @NonNull final Function<? super V, ? extends T> converter) {
            super();

            mLens = lens;
            mConverter = converter;
        }

        @Nullable
        @Override
        public final T get(@NonNull final M model) {
            final V value = mLens.get(model);
            return (value == null) ? null : mConverter.invoke(value);
        }
    }

    private static class WriteConversion<M, V, T> extends Lens.Write.Base<M, T> {

        @NonNull
        private final Lens.Write<M, V> mLens;
        @NonNull
        private final Function<? super T, ? extends V> mConverter;

        private WriteConversion(@NonNull final Lens.Write<M, V> lens,
                                @NonNull final Function<? super T, ? extends V> converter) {
            super();

            mLens = lens;
            mConverter = converter;
        }

        @Override
        public final void set(@NonNull final M model, @Nullable final T value) {
            mLens.set(model, (value == null) ? null : mConverter.invoke(value));
        }
    }

    private static class Combine<M, V> extends Lens.ReadWrite.Base<M, V> {

        @NonNull
        private final Lens.Read<M, ? extends V> mRead;
        @NonNull
        private final Lens.Write<M, ? super V> mWrite;

        private Combine(@NonNull final Lens.Read<M, ? extends V> read,
                        @NonNull final Lens.Write<M, ? super V> write) {
            super();

            mRead = read;
            mWrite = write;
        }

        @Nullable
        @Override
        public final V get(@NonNull final M model) {
            return mRead.get(model);
        }

        @Override
        public final void set(@NonNull final M model, @Nullable final V value) {
            mWrite.set(model, value);
        }
    }

    private Lenses() {
        super();
    }
}
