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

package android.orm.remote.route;

import android.orm.sql.Column;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static android.orm.remote.route.Path.Root;

public final class Paths {

    @NonNull
    public static Path path(@NonNls @NonNull final String literal) {
        return path(new Segment.Literal(literal));
    }

    @NonNull
    public static <V> Path path(@NonNull final Column<V> column) {
        return path(Argument.isEqualTo(column));
    }

    @NonNull
    public static Path path(@NonNull final Segment segment) {
        return Root.slash(segment);
    }

    private Paths() {
        super();
    }
}
