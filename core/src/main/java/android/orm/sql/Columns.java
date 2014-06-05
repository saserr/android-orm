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

package android.orm.sql;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.orm.sql.statement.Select;
import android.orm.util.Maybe;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.math.BigDecimal;

import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.orm.sql.Column.column;
import static android.orm.sql.Types.Binary;
import static android.orm.sql.Types.Bool;
import static android.orm.sql.Types.Decimal;
import static android.orm.sql.Types.File;
import static android.orm.sql.Types.Integer;
import static android.orm.sql.Types.Real;
import static android.orm.sql.Types.Text;
import static android.orm.sql.Types.Uri;

public final class Columns {

    public static final Column<Long> Id = column(BaseColumns._ID, Integer)
            .asUnique()
            .asNotNull();

    @NonNull
    public static <V> Value.Read<V> readAs(@NonNls @NonNull final String name,
                                           @NonNull final Column<V> column) {
        return new ReadAs<>(name, column);
    }

    @NonNull
    public static Column<String> text(@NonNls @NonNull final String name) {
        return column(name, Text);
    }

    @NonNull
    public static Column<Long> number(@NonNls @NonNull final String name) {
        return column(name, Integer);
    }

    @NonNull
    public static Column<Double> real(@NonNls @NonNull final String name) {
        return column(name, Real);
    }

    @NonNull
    public static Column<Boolean> bool(@NonNls @NonNull final String name) {
        return column(name, Bool);
    }

    @NonNull
    public static Column<BigDecimal> decimal(@NonNls @NonNull final String name) {
        return column(name, Decimal);
    }

    @NonNull
    public static <E extends Enum<E>> Column<E> enumName(@NonNull final Class<E> klass,
                                                         @NonNls @NonNull final String name) {
        return column(name, Types.enumName(klass));
    }

    @NonNull
    public static Column<File> file(@NonNls @NonNull final String name) {
        return column(name, File);
    }

    @NonNull
    public static Column<Uri> uri(@NonNls @NonNull final String name) {
        return column(name, Uri);
    }

    @NonNull
    public static Column<Pair<File, byte[]>> binary(@NonNls @NonNull final String name) {
        return column(name, Binary);
    }

    @NonNull
    public static Column<Pair<File, Bitmap>> bitmap(@NonNls @NonNull final String name) {
        return bitmap(name, PNG, 100, null);
    }

    @NonNull
    public static Column<Pair<File, Bitmap>> bitmap(@NonNls @NonNull final String name,
                                                    @NonNull final Bitmap.CompressFormat format,
                                                    final int quality) {
        return bitmap(name, format, quality, null);
    }

    @NonNull
    public static Column<Pair<File, Bitmap>> bitmap(@NonNls @NonNull final String name,
                                                    @Nullable final BitmapFactory.Options options) {
        return bitmap(name, PNG, 100, options);
    }

    @NonNull
    public static Column<Pair<File, Bitmap>> bitmap(@NonNls @NonNull final String name,
                                                    @NonNull final Bitmap.CompressFormat format,
                                                    final int quality,
                                                    @Nullable final BitmapFactory.Options options) {
        return column(name, Types.bitmap(format, quality, options));
    }

    private static class ReadAs<V> extends Value.Read.Base<V> {

        private final String mName;
        private final Type<V> mType;
        private final Select.Projection mProjection;

        private ReadAs(@NonNls @NonNull final String newName,
                       @NonNls @NonNull final Column<V> column) {
            super();

            mName = newName;
            mType = column.getType();
            mProjection = Select.projection(newName, Helper.escape(column.getName()));
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
        public final Maybe<V> read(@NonNull final Readable input) {
            return mType.read(input, mName);
        }
    }

    private Columns() {
        super();
    }
}
