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
import android.orm.model.Plan;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Validation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NonNls;

import static android.orm.gson.Deserializers.lens;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Deserializer<E extends JsonElement> extends Mapper.Write.Base<E> {

    private static final String TAG = Deserializer.class.getSimpleName();

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Plan.Write.Builder<E> mPlan;

    public Deserializer(@NonNull final Plan.Write.Builder<E> plan) {
        this(TAG, plan);
    }

    public Deserializer(@NonNls @NonNull final String name,
                        @NonNull final Plan.Write.Builder<E> plan) {
        super();

        mName = name;
        mPlan = new Plan.Write.Builder<>(plan);
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    @Override
    public final Writer prepareWriter(@NonNull final Maybe<E> value) {
        return mPlan.build(value);
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
            mPlan = new Plan.Write.Builder<>();
        }

        @NonNull
        public final Builder with(@NonNull final Writer writer) {
            mPlan.with(writer);
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
            mPlan.with(value, lens(mGson, klass, name));
            return this;
        }

        @NonNull
        public final <M> Builder with(@NonNull final Class<M> klass,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Mapper.Write<M> mapper) {
            mPlan.with(mapper, lens(mGson, klass, name));
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
            mPlan.with(value, lens(mGson, klass, mName + '.' + name, name, validation));
            return this;
        }

        @NonNull
        public final <M> Builder with(@NonNull final Class<M> klass,
                                      @NonNls @NonNull final String name,
                                      @NonNull final Mapper.Write<M> mapper,
                                      @NonNull final Validation<? super M> validation) {
            mPlan.with(mapper, lens(mGson, klass, mName + '.' + name, name, validation));
            return this;
        }

        @NonNull
        public final Builder with(@NonNls @NonNull final String name,
                                  @NonNull final Value.Write<JsonObject> value) {
            mPlan.with(value, property(name));
            return this;
        }

        @NonNull
        public final Builder with(@NonNls @NonNull final String name,
                                  @NonNull final Mapper.Write<JsonObject> mapper) {
            mPlan.with(mapper, property(name));
            return this;
        }

        @NonNull
        public final Deserializer<JsonObject> build() {
            return new Deserializer<>(mName, mPlan);
        }

        @NonNull
        private static Lens.Read<JsonObject, Maybe<JsonObject>> property(@NonNls @NonNull final String name) {
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
}
