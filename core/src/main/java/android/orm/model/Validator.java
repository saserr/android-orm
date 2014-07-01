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

package android.orm.model;

import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Validation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.orm.util.Maybes.something;

public final class Validator {

    public interface Callback<V> {

        void onValid(@Nullable final V v);

        void onInvalid(@Nullable final V v);
    }

    public interface Instance {

        boolean isValid();

        class Builder {

            private final Collection<Entry> mEntries = new ArrayList<>();

            @NonNull
            public final <V> Builder with(@NonNull final Validation<? super V> validation,
                                          @NonNull final android.orm.model.Instance.Getter<V> getter,
                                          @NonNull final Callback<V> callback) {
                return with(entry(validation, getter, callback));
            }

            @NonNull
            public final <V> Builder with(@NonNull final Validation<? super V> validation,
                                          @NonNull final Binding.Read<V> binding,
                                          @NonNull final Callback<Maybe<V>> callback) {
                return with(entry(validation, binding, callback));
            }

            @NonNull
            public final Instance build() {
                return Validator.build(new ArrayList<>(mEntries));
            }

            @NonNull
            private Builder with(@NonNull final Entry entry) {
                mEntries.add(entry);
                return this;
            }

            private static <V> Entry entry(@NonNull final Validation<? super V> validation,
                                           @NonNull final android.orm.model.Instance.Getter<V> getter,
                                           @NonNull final Callback<V> callback) {
                return new Entry() {
                    @Override
                    public boolean isValid() {
                        final V value = getter.get();
                        final boolean valid = validation.isValid(something(value));

                        if (valid) {
                            callback.onValid(value);
                        } else {
                            callback.onInvalid(value);
                        }

                        return valid;
                    }
                };
            }

            private static <V> Entry entry(@NonNull final Validation<? super V> validation,
                                           @NonNull final Binding.Read<V> binding,
                                           @NonNull final Callback<Maybe<V>> callback) {
                return new Entry() {
                    @Override
                    public boolean isValid() {
                        final Maybe<V> value = binding.get();
                        final boolean valid = validation.isValid(value);

                        if (valid) {
                            callback.onValid(value);
                        } else {
                            callback.onInvalid(value);
                        }

                        return valid;
                    }
                };
            }

            private interface Entry {
                boolean isValid();
            }
        }
    }

    public interface Type<V> {

        boolean isValid(@Nullable final V value);

        class Builder<V> {

            private final Collection<Entry<V>> mEntries = new ArrayList<>();

            @NonNull
            public final Builder<V> with(@NonNull final Validation<? super V> validation,
                                         @NonNull final Callback<V> callback) {
                return with(entry(validation, callback));
            }

            @NonNull
            public final <T> Builder<V> with(@NonNull final Validation<? super T> validation,
                                             @NonNull final Lens.Read<V, T> lens,
                                             @NonNull final Callback<V> callback) {
                return with(entry(validation, lens, callback));
            }

            @NonNull
            public final Type<V> build() {
                return build(new ArrayList<>(mEntries));
            }

            @NonNull
            public final Instance build(@Nullable final V value) {
                return Validator.build(value, new ArrayList<>(mEntries));
            }

            @NonNull
            private Builder<V> with(@NonNull final Entry<V> entry) {
                mEntries.add(entry);
                return this;
            }

            @NonNull
            private static <V> Entry<V> entry(@NonNull final Validation<? super V> validation,
                                              @NonNull final Callback<V> callback) {
                return new Entry<V>() {
                    @Override
                    public boolean isValid(@Nullable final V value) {
                        final boolean valid = validation.isValid(something(value));

                        if (valid) {
                            callback.onValid(value);
                        } else {
                            callback.onInvalid(value);
                        }

                        return valid;
                    }
                };
            }

            @NonNull
            private static <V, T> Entry<V> entry(@NonNull final Validation<? super T> validation,
                                                 @NonNull final Lens.Read<V, T> lens,
                                                 @NonNull final Callback<V> callback) {
                return new Entry<V>() {
                    @Override
                    public boolean isValid(@Nullable final V value) {
                        final T t = (value == null) ? null : lens.get(value);
                        final boolean valid = validation.isValid(something(t));

                        if (valid) {
                            callback.onValid(value);
                        } else {
                            callback.onInvalid(value);
                        }

                        return valid;
                    }
                };
            }

            @NonNull
            private static <V> Type<V> build(@NonNull final List<Entry<V>> entries) {
                return new Type<V>() {
                    @Override
                    public boolean isValid(@Nullable final V value) {
                        boolean valid = true;

                        final int size = entries.size();
                        for (int i = 0; (i < size) && valid; i++) {
                            valid = entries.get(i).isValid(value);
                        }

                        return valid;
                    }
                };
            }

            private interface Entry<V> {
                boolean isValid(@Nullable final V value);
            }
        }
    }

    @NonNull
    public static Instance.Builder instance() {
        return new Instance.Builder();
    }

    @NonNull
    public static <V> Type.Builder<V> type() {
        return new Type.Builder<>();
    }

    @NonNull
    private static Instance build(@NonNull final List<Instance.Builder.Entry> entries) {
        return new Instance() {
            @Override
            public boolean isValid() {
                boolean valid = true;

                final int size = entries.size();
                for (int i = 0; (i < size) && valid; i++) {
                    valid = entries.get(i).isValid();
                }

                return valid;
            }
        };
    }

    @NonNull
    private static <V> Instance build(@Nullable final V value,
                                      @NonNull final Collection<Type.Builder.Entry<V>> entries) {
        final List<Instance.Builder.Entry> bounded = new ArrayList<>(entries.size());

        for (final Type.Builder.Entry<V> entry : entries) {
            bounded.add(bind(value, entry));
        }

        return build(bounded);
    }

    @NonNull
    private static <V> Instance.Builder.Entry bind(@Nullable final V value,
                                                   @NonNull final Type.Builder.Entry<V> entry) {
        return new Instance.Builder.Entry() {
            @Override
            public boolean isValid() {
                return entry.isValid(value);
            }
        };
    }

    private Validator() {
        super();
    }
}
