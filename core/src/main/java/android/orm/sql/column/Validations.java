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

package android.orm.sql.column;

import android.database.SQLException;
import android.orm.sql.Value;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Value.Write.Operation.Update;

public final class Validations {

    public static final Validation<Object> NonUpdateable = new Validation<Object>() {

        @Override
        public void afterRead(@NonNls @NonNull final String name,
                              @NonNull final Maybe<?> value) {/*do nothing */}

        @Override
        public void beforeWrite(@NonNull final Value.Write.Operation operation,
                                @NonNls @NonNull final String name,
                                @NonNull final Maybe<?> value) {
            if ((operation == Update) && value.isSomething()) {
                throw new SQLException(name + ": should not be updated!");
            }
        }
    };

    @NonNull
    public static <V> Validation<V> convert(@NonNull final android.orm.util.Validation<? super V> validation) {
        return new Conversion<>(validation);
    }

    @NonNull
    public static <V> Validation<V> compose(@NonNull final Validation<V> first,
                                            @NonNull final Validation<? super V> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Validation<V> safeCast(@NonNull final Validation<? super V> validation) {
        return (Validation<V>) validation;
    }

    private static class Conversion<V> implements Validation<V> {

        @NonNull
        private final android.orm.util.Validation<? super V> mValidation;

        private Conversion(@NonNull final android.orm.util.Validation<? super V> validation) {
            super();

            mValidation = validation;
        }

        @Override
        public final void afterRead(@NonNls @NonNull final String name,
                                    @NonNull final Maybe<? extends V> value) {
            if (value.isSomething()) {
                mValidation.validate(value);
            }
        }

        @Override
        public final void beforeWrite(@NonNull final Value.Write.Operation operation,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Maybe<? extends V> value) {
            if (value.isSomething()) {
                mValidation.validate(value);
            }
        }
    }

    private static class Composition<V> implements Validation<V> {

        @NonNull
        private final Validation<V> mFirst;
        @NonNull
        private final Validation<? super V> mSecond;

        private Composition(@NonNull final Validation<V> first,
                            @NonNull final Validation<? super V> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @Override
        public final void afterRead(@NonNls @NonNull final String name,
                                    @NonNull final Maybe<? extends V> value) {
            mFirst.afterRead(name, value);
            mSecond.afterRead(name, value);
        }

        @Override
        public final void beforeWrite(@NonNull final Value.Write.Operation operation,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Maybe<? extends V> value) {
            mFirst.beforeWrite(operation, name, value);
            mSecond.beforeWrite(operation, name, value);
        }
    }

    private Validations() {
        super();
    }
}
