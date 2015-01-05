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

import java.util.Collection;
import java.util.LinkedList;

import static android.orm.model.Validators.validator;

public interface Validator {

    boolean isValid();

    class Builder {

        private final Collection<Validator> mValidators = new LinkedList<>();

        @NonNull
        public final Builder with(@NonNull final Validator validator) {
            mValidators.add(validator);
            return this;
        }

        @NonNull
        public final <V> Builder with(@NonNull final Instance.Getter<V> getter,
                                      @NonNull final Validation<? super V> validation) {
            return with(validator(getter, validation));
        }

        @NonNull
        public final <V> Builder with(@NonNull final Binding.Read<V> binding,
                                      @NonNull final Validation<? super V> validation) {
            return with(validator(binding, validation));
        }

        @NonNull
        public final Validator build() {
            return Validators.combine(mValidators.toArray(new Validator[mValidators.size()]));
        }
    }
}
