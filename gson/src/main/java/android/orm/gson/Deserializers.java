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

package android.orm.gson;

import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.model.Plan;
import android.orm.sql.Value;
import android.orm.util.Lens;
import android.orm.util.Maybes;
import android.orm.util.Validation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public final class Deserializers {

    @NonNull
    public static <V> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<V> klass,
                                                     @NonNull final Value.Write<V> value) {
        return from(gson, klass, Mappers.write(value));
    }

    @NonNull
    public static <M> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<M> klass,
                                                     @NonNull final Mapper.Write<M> mapper) {
        return new Deserializer<>(Plan.Write.<JsonElement>builder()
                .put(mapper, Maybes.lift(new Lens.Read<JsonElement, M>() {
                    @Nullable
                    @Override
                    public M get(@NonNull final JsonElement json) {
                        return gson.fromJson(json, klass);
                    }
                }))
        );
    }

    @NonNull
    public static <V> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<V> klass,
                                                     @NonNull final Value.Write<V> value,
                                                     @NonNull final Validation<? super V> validation) {
        return from(gson, klass, Mappers.write(value), validation);
    }

    @NonNull
    public static <M> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<M> klass,
                                                     @NonNull final Mapper.Write<M> mapper,
                                                     @NonNull final Validation<? super M> validation) {
        return new Deserializer<>(Plan.Write.<JsonElement>builder()
                .put(mapper, Maybes.lift(new Lens.Read<JsonElement, M>() {

                    private final Validation<? super M> mValidation = validation.name(mapper.getName());

                    @Nullable
                    @Override
                    public M get(@NonNull final JsonElement json) {
                        return mValidation.validate(gson.fromJson(json, klass)).get();
                    }
                }))
        );
    }

    private Deserializers() {
        super();
    }
}
