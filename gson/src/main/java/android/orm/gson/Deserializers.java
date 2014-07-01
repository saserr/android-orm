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
import android.orm.model.Plan;
import android.orm.sql.Value;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Validation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Lenses.convert;
import static android.orm.util.Maybes.something;

public final class Deserializers {

    @NonNull
    public static <V> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<V> klass,
                                                     @NonNull final Value.Write<V> value) {
        return new Deserializer<>(Plan.Write.<JsonElement>builder().put(value, lens(gson, klass)));
    }

    @NonNull
    public static <M> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<M> klass,
                                                     @NonNull final Mapper.Write<M> mapper) {
        return new Deserializer<>(Plan.Write.<JsonElement>builder().put(mapper, lens(gson, klass)));
    }

    @NonNull
    public static <V> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<V> klass,
                                                     @NonNull final Value.Write<V> value,
                                                     @NonNull final Validation<? super V> validation) {

        return new Deserializer<>(Plan.Write.<JsonElement>builder()
                .put(value, lens(gson, klass, value.getName(), validation)));
    }

    @NonNull
    public static <M> Deserializer<JsonElement> from(@NonNull final Gson gson,
                                                     @NonNull final Class<M> klass,
                                                     @NonNull final Mapper.Write<M> mapper,
                                                     @NonNull final Validation<? super M> validation) {
        return new Deserializer<>(Plan.Write.<JsonElement>builder()
                .put(mapper, lens(gson, klass, mapper.getName(), validation)));
    }

    @NonNull
    public static <V> Lens.Read<JsonElement, Maybe<V>> lens(@NonNull final Gson gson,
                                                            @NonNull final Class<V> klass) {
        return Maybes.lift(new Lens.Read<JsonElement, V>() {
            @Nullable
            @Override
            public V get(@NonNull final JsonElement json) {
                return gson.fromJson(json, klass);
            }
        });
    }

    @NonNull
    public static <V> Lens.Read<JsonElement, Maybe<V>> lens(@NonNull final Gson gson,
                                                            @NonNull final Class<V> klass,
                                                            @NonNls @NonNull final String name,
                                                            @NonNull final Validation<? super V> validation) {
        return Maybes.lift(new Lens.Read<JsonElement, V>() {

            private final Validation<? super V> mValidation = validation.name(name);

            @Nullable
            @Override
            public V get(@NonNull final JsonElement json) {
                final V value = gson.fromJson(json, klass);
                mValidation.isValidOrThrow(something(value));
                return value;
            }
        });
    }

    @NonNull
    public static <V> Lens.Read<JsonObject, Maybe<V>> lens(@NonNull final Gson gson,
                                                           @NonNull final Class<V> klass,
                                                           @NonNls @NonNull final String name) {
        return new Lens.Read<JsonObject, Maybe<V>>() {
            @Nullable
            @Override
            public Maybe<V> get(@NonNull final JsonObject json) {
                return json.has(name) ?
                        something(gson.fromJson(json.get(name), klass)) :
                        Maybes.<V>nothing();
            }
        };
    }

    @NonNull
    public static <V> Lens.Read<JsonObject, Maybe<V>> lens(@NonNull final Gson gson,
                                                           @NonNull final Class<V> klass,
                                                           @NonNls @NonNull final String qualifiedName,
                                                           @NonNls @NonNull final String elementName,
                                                           @NonNull final Validation<? super V> validation) {
        return convert(lens(gson, klass, elementName), new Function<Maybe<V>, Maybe<V>>() {

            private final Validation<? super V> mValidation = validation.name(qualifiedName);

            @NonNull
            @Override
            public Maybe<V> invoke(@NonNull final Maybe<V> value) {
                mValidation.isValidOrThrow(value);
                return value;
            }
        });
    }

    private Deserializers() {
        super();
    }
}
