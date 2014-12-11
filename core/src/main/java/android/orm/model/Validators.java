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

import android.orm.util.Validation;
import android.support.annotation.NonNull;

import static android.orm.util.Maybes.something;

public final class Validators {

    @NonNull
    public static <V> Validator validator(@NonNull final Instance.Getter<V> getter,
                                          @NonNull final Validation<? super V> validation) {
        return new Validator() {
            @Override
            public boolean isValid() {
                return validation.isValid(something(getter.get()));
            }
        };
    }

    @NonNull
    public static <V> Validator validator(@NonNull final Binding.Read<V> binding,
                                          @NonNull final Validation<? super V> validation) {
        return new Validator() {
            @Override
            public boolean isValid() {
                return validation.isValid(binding.get());
            }
        };
    }

    @NonNull
    public static Validator combine(@NonNull final Validator... validators) {
        return new Validator() {

            private final int mLength = validators.length;

            @Override
            public boolean isValid() {
                boolean valid = true;

                for (int i = 0; (i < mLength) && valid; i++) {
                    valid = validators[i].isValid();
                }

                return valid;
            }
        };
    }

    private Validators() {
        super();
    }
}
