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

package android.orm.gson;

import android.database.SQLException;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.model.Plan;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.Writer;
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
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Deserializer<E extends JsonElement> extends Value.Write.Base<E> {

    private static final String TAG = Deserializer.class.getSimpleName();

    @NonNull
    private final Plan.Write.Builder<E> mPlan;

    public Deserializer(@NonNull final Plan.Write.Builder<E> plan) {
        super();

        mPlan = new Plan.Write.Builder<>(plan);
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return TAG;
    }

    @Override
    public final void write(@NonNull final Operation operation,
                            @NonNull final Maybe<E> value,
                            @NonNull final Writable output) {
        mPlan.build(value).write(operation, output);
    }

    @NonNull
    public static Builder builder(@NonNls @NonNull final String name, @NonNull final Gson gson) {
        return new Builder(name, gson);
    }

    public static class Builder {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Gson mGson;
        @NonNull
        private final Plan.Write.Builder<JsonObject> mPlan;

        public Builder(@NonNls @NonNull final String name,
                       @NonNull final Gson gson) {
            super();

            mName = name;
            mGson = gson;
            mPlan = Plan.Write.builder();
        }

        @NonNull
        public final Builder with(@NonNull final Writer writer) {
            mPlan.put(writer);
            return this;
        }

        @NonNull
        public final <V> Builder with(@NonNull final Class<V> klass,
                                      @NonNull final Value.Write<V> value) {
            return with(klass, value.getName(), value);
        }

        @NonNull
        public final <V> Builder with(@NonNull final Class<V> klass,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Value.Write<V> value) {
            return with(klass, name, Mappers.write(value));
        }

        @NonNull
        public final <M> Builder with(@NonNull final Class<M> klass,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Mapper.Write<M> mapper) {
            mPlan.put(mapper, Deserializer.lens(mGson, klass, name));
            return this;
        }

        @NonNull
        public final <V> Builder with(@NonNull final Class<V> klass,
                                      @NonNull final Value.Write<V> value,
                                      @NonNull final Validation<? super V> validation) {
            return with(klass, value.getName(), value, validation);
        }

        @NonNull
        public final <V> Builder with(@NonNull final Class<V> klass,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Value.Write<V> value,
                                      @NonNull final Validation<? super V> validation) {
            return with(klass, name, Mappers.write(value), validation);
        }

        @NonNull
        public final <M> Builder with(@NonNull final Class<M> klass,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Mapper.Write<M> mapper,
                                      @NonNull final Validation<? super M> validation) {
            mPlan.put(mapper, Deserializer.lens(mGson, klass, mName + '.' + name, name, validation));
            return this;
        }

        @NonNull
        public final Builder with(@NonNls @NonNull final String name,
                                  @NonNull final Value.Write<JsonObject> value) {
            return with(name, Mappers.write(value));
        }

        @NonNull
        public final Builder with(@NonNls @NonNull final String name,
                                  @NonNull final Mapper.Write<JsonObject> mapper) {
            mPlan.put(mapper, lens(name));
            return this;
        }

        @NonNull
        public final Deserializer<JsonObject> build() {
            return new Deserializer<>(mPlan);
        }

        @NonNull
        private static Lens.Read<JsonObject, Maybe<JsonObject>> lens(@NonNls @NonNull final String name) {
            return new Lens.Read<JsonObject, Maybe<JsonObject>>() {
                @Nullable
                @Override
                public Maybe<JsonObject> get(@NonNull final JsonObject json) {
                    final Maybe<JsonObject> result;

                    if (json.has(name)) {
                        final JsonElement element = json.get(name);
                        if (!element.isJsonNull() && !element.isJsonObject()) {
                            throw new SQLException(name + " is not a json object");
                        }
                        result = something(element.isJsonNull() ? null : element.getAsJsonObject());
                    } else {
                        result = nothing();
                    }

                    return result;
                }
            };
        }
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
                return mValidation.validate(value).get();
            }
        });
    }
}
