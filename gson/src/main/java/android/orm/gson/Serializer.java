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

import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.sql.Readable;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Lenses.combine;
import static android.orm.util.Maybes.something;

public class Serializer<E extends JsonElement> extends Value.Read.Base<E> {

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Reading.Item.Create<E> mReading;
    @NonNull
    private final Select.Projection mProjection;

    public Serializer(@NonNls @NonNull final String name,
                      @NonNull final Reading.Item.Create<E> reading) {
        super();

        mName = name;
        mReading = reading;
        mProjection = mReading.getProjection();
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    @Override
    public final Select.Projection getProjection() {
        return mProjection;
    }

    @NonNull
    @Override
    public final Maybe<E> read(@NonNull final Readable input) {
        return mReading.read(input).produce();
    }

    @NonNull
    public static Builder builder(@NonNls @NonNull final String name,
                                  @NonNull final Gson gson) {
        return new Builder(name, gson);
    }

    public static class Builder {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Gson mGson;
        @NonNull
        private final Reading.Item.Builder<JsonObject> mReading;

        public Builder(@NonNls @NonNull final String name,
                       @NonNull final Gson gson) {
            super();

            mName = name;
            mGson = gson;
            mReading = Reading.Item.builder(producer(name));
        }

        public final <V> Builder with(@NonNull final Value.Read<V> value) {
            return with(value.getName(), value);
        }

        public final <V> Builder with(@NonNls @NonNull final String name,
                                      @NonNull final Value.Read<V> value) {
            mReading.with(value, Serializer.<V>lens(mGson, name));
            return this;
        }

        public final <M> Builder with(@NonNls @NonNull final String name,
                                      @NonNull final Class<M> klass,
                                      @NonNull final Mapper.Read<M> mapper) {
            mReading.with(mapper, combine(Deserializer.lens(mGson, klass, name), Serializer.<M>lens(mGson, name)));
            return this;
        }

        public final Serializer<JsonObject> build() {
            return new Serializer<>(mName, mReading.build());
        }

        private static Value.Read<JsonObject> producer(@NonNls @NonNull final String name) {
            return new Base<JsonObject>() {

                @NonNls
                @NonNull
                @Override
                public String getName() {
                    return name;
                }

                @NonNull
                @Override
                public Select.Projection getProjection() {
                    return Select.Projection.Nothing;
                }

                @NonNull
                @Override
                public Maybe<JsonObject> read(@NonNull final Readable input) {
                    return something(new JsonObject());
                }
            };
        }
    }

    public static <V> Lens.Write<JsonObject, Maybe<V>> lens(@NonNull final Gson gson,
                                                            @NonNls @NonNull final String name) {
        return new Lens.Write<JsonObject, Maybe<V>>() {
            @Override
            public void set(@NonNull final JsonObject json, @Nullable final Maybe<V> value) {
                if (value == null) {
                    json.add(name, JsonNull.INSTANCE);
                } else if (value.isSomething()) {
                    json.add(name, gson.toJsonTree(value.get()));
                }
            }
        };
    }
}
