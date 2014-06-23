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

package android.orm.model;

import android.orm.sql.Select;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public final class Instance {

    public interface Getter<V> {
        @NonNull
        Maybe<V> get();
    }

    public interface Setter<V> {
        void set(@NonNull final Maybe<V> v);
    }

    public interface Readable {

        @NonNls
        @NonNull
        String name();

        @NonNull
        Select.Projection projection();

        @NonNull
        Reading.Item.Action prepareRead();
    }

    public interface Writable {
        @NonNull
        Plan.Write prepareWrite();
    }

    public interface ReadWrite extends Readable, Writable {
    }

    private Instance() {
        super();
    }
}
