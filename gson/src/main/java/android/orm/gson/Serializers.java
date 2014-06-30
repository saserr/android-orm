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

import android.orm.model.Reading;
import android.orm.sql.Value;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NonNls;

public final class Serializers {

    private static final Object TO_ARRAY = new Function<Iterable<JsonElement>, JsonArray>() {
        @NonNull
        @Override
        public JsonArray invoke(@NonNull final Iterable<JsonElement> elements) {
            final JsonArray array = new JsonArray();

            for (final JsonElement element : elements) {
                array.add(element);
            }

            return array;
        }
    };

    @NonNull
    @SuppressWarnings("unchecked")
    public static <E extends JsonElement, C extends Iterable<E>> Function<C, JsonArray> toArray() {
        return (Function<C, JsonArray>) TO_ARRAY;
    }

    @NonNull
    public static <V> Serializer<JsonElement> from(@NonNull final Gson gson,
                                                   @NonNull final Value.Read<V> value) {
        return new Serializer<>(value.getName(), Reading.Item.builder(value.mapTo(new ToJson<V>(gson))));
    }

    @NonNull
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

    private static class ToJson<V> implements Function<V, JsonElement> {

        @NonNull
        private final Gson mGson;

        private ToJson(@NonNull final Gson gson) {
            super();

            mGson = gson;
        }

        @NonNull
        @Override
        public final JsonElement invoke(@NonNull final V value) {
            return mGson.toJsonTree(value);
        }
    }

    private Serializers() {
        super();
    }
}
