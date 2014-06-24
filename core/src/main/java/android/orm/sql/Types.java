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

package android.orm.sql;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.orm.util.Converter;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import static android.database.DatabaseUtils.sqlEscapeString;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;

public final class Types {

    public static final Type<String> Text = new Type.Base<String>("text", "*") {

        @NonNls
        @NonNull
        @Override
        public String fromString(@NonNull final String value) {
            return value;
        }

        @NonNls
        @NonNull
        @Override
        public String toString(@NonNull final String value) {
            return value;
        }

        @NonNls
        @NonNull
        @Override
        public String escape(@NonNull final String value) {
            return sqlEscapeString(value);
        }

        @NonNull
        @Override
        public Maybe<String> read(@NonNull final Readable input,
                                  @NonNls @NonNull final String name) {
            return input.getAsString(name);
        }

        @Override
        public void write(@NonNull final Writable output,
                          @NonNls @NonNull final String name,
                          @NonNull final String value) {
            output.put(name, value);
        }
    };

    public static final Type<Long> Integer = new Type.Base<Long>("integer", "#") {

        @NonNull
        @Override
        public Long fromString(@NonNull final String value) {
            return parseLong(value);
        }

        @NonNls
        @NonNull
        @Override
        public String toString(@NonNull final Long value) {
            return value.toString();
        }

        @NonNls
        @NonNull
        @Override
        public String escape(@NonNull final Long value) {
            return value.toString();
        }

        @NonNull
        @Override
        public Maybe<Long> read(@NonNull final Readable input,
                                @NonNls @NonNull final String name) {
            return input.getAsLong(name);
        }

        @Override
        public void write(@NonNull final Writable output,
                          @NonNls @NonNull final String name,
                          @NonNull final Long value) {
            output.put(name, value);
        }
    };

    public static final Type<Double> Real = new Type.Base<Double>("real", "*") {

        @NonNull
        @Override
        public Double fromString(@NonNull final String value) {
            return parseDouble(value);
        }

        @NonNls
        @NonNull
        @Override
        public String toString(@NonNull final Double value) {
            return value.toString();
        }

        @NonNls
        @NonNull
        @Override
        public String escape(@NonNull final Double value) {
            return value.toString();
        }

        @NonNull
        @Override
        public Maybe<Double> read(@NonNull final Readable input,
                                  @NonNls @NonNull final String name) {
            return input.getAsDouble(name);
        }

        @Override
        public void write(@NonNull final Writable output,
                          @NonNls @NonNull final String name,
                          @NonNull final Double value) {
            output.put(name, value);
        }
    };

    @NonNull
    public static <V, T> Type<V> map(@NonNull final Type<T> type,
                                     @NonNull final Converter<V, T> converter) {
        return new Conversion<>(type, converter);
    }

    public static final Type<Boolean> Bool = map(
            Integer,
            new Converter<Boolean, Long>() {

                private static final long False = 0L;
                private static final long True = 1L;

                @NonNull
                @Override
                public Long from(@NonNull final Boolean value) {
                    return value ? True : False;
                }

                @NonNull
                @Override
                public Boolean to(@NonNull final Long value) {
                    return value != False;
                }
            }
    );

    public static final Type<BigDecimal> Decimal = map(
            Text,
            new Converter<BigDecimal, String>() {

                @NonNull
                @Override
                public String from(@NonNull final BigDecimal value) {
                    return value.toPlainString();
                }

                @NonNull
                @Override
                public BigDecimal to(@NonNull final String value) {
                    return new BigDecimal(value);
                }
            }
    );

    public static final Type<File> File = map(
            Text,
            new Converter<File, String>() {

                @NonNull
                @Override
                public String from(@NonNull final File file) {
                    return file.getPath();
                }

                @NonNull
                @Override
                public File to(@NonNull final String path) {
                    return new File(path);
                }
            }
    );

    public static final Type<Uri> Uri = map(
            Text,
            new Converter<Uri, String>() {

                @NonNull
                @Override
                public String from(@NonNull final Uri uri) {
                    return uri.toString();
                }

                @NonNull
                @Override
                public Uri to(@NonNull final String value) {
                    return android.net.Uri.parse(value);
                }
            }
    );

    public static final Type<Pair<File, byte[]>> Binary = map(
            File,
            new Converter<Pair<File, byte[]>, File>() {

                @NonNull
                @Override
                public File from(@NonNull final Pair<File, byte[]> pair) {
                    try {
                        final FileOutputStream out = new FileOutputStream(pair.first);
                        try {
                            out.write(pair.second, 0, pair.second.length);
                        } finally {
                            out.close();
                        }
                    } catch (final IOException ex) {
                        // TODO log
                    }

                    return pair.first;
                }

                @NonNull
                @Override
                public Pair<File, byte[]> to(@NonNull final File file) {
                    byte[] bytes = null;

                    try {
                        final ByteArrayOutputStream out = new ByteArrayOutputStream();
                        final InputStream in = new FileInputStream(file);
                        try {
                            final byte[] buffer = new byte[1024];
                            int read = in.read(buffer);
                            while (read != -1) {
                                out.write(buffer, 0, read);
                                read = in.read(buffer);
                            }
                            bytes = out.toByteArray();
                        } finally {
                            in.close();
                            out.close();
                        }
                    } catch (final IOException ex) {
                        // TODO log
                    }

                    return Pair.create(file, bytes);
                }
            }
    );

    @NonNull
    public static <E extends Enum<E>> Type<E> enumName(@NonNull final Class<E> klass) {
        return map(Text, new Converter<E, String>() {

            @NonNull
            @Override
            public String from(@NonNull final E value) {
                return value.name();
            }

            @NonNull
            @Override
            public E to(@NonNull final String value) {
                return Enum.valueOf(klass, value);
            }
        });
    }

    public static Type<Pair<File, Bitmap>> bitmap(@NonNull final Bitmap.CompressFormat format,
                                                  final int quality,
                                                  @Nullable final BitmapFactory.Options options) {
        return map(
                File,
                new Converter<Pair<File, Bitmap>, File>() {

                    @NonNull
                    @Override
                    public File from(@NonNull final Pair<File, Bitmap> pair) {
                        try {
                            final FileOutputStream out = new FileOutputStream(pair.first);
                            try {
                                pair.second.compress(format, quality, out);
                            } finally {
                                out.close();
                            }
                        } catch (final IOException ex) {
                            // TODO log
                        }

                        return pair.first;
                    }

                    @NonNull
                    @Override
                    public Pair<File, Bitmap> to(@NonNull final File file) {
                        return Pair.create(file, BitmapFactory.decodeFile(file.getAbsolutePath(), options));
                    }
                }
        );
    }

    private static class Conversion<V, T> extends Type.Base<V> {

        @NonNull
        private final Type<T> mType;
        @NonNull
        private final Converter<V, T> mConverter;

        private Conversion(@NonNull final Type<T> type, @NonNull final Converter<V, T> converter) {
            super(type.toSQL(), type.getWildcard());

            mType = type;
            mConverter = converter;
        }

        @NonNull
        @Override
        public final V fromString(@NonNull final String value) {
            return mConverter.to(mType.fromString(value));
        }

        @NonNls
        @NonNull
        @Override
        public final String toString(@NonNull final V value) {
            return mType.toString(mConverter.from(value));
        }

        @NonNls
        @NonNull
        @Override
        public final String escape(@NonNull final V value) {
            return mType.escape(mConverter.from(value));
        }

        @NonNull
        @Override
        public final Maybe<V> read(@NonNull final Readable input,
                                   @NonNls @NonNull final String name) {
            final Maybe<V> result;

            final Maybe<T> read = mType.read(input, name);
            if (read.isSomething()) {
                final T value = read.get();
                result = something((value == null) ? null : mConverter.to(value));
            } else {
                result = nothing();
            }

            return result;
        }

        @Override
        public final void write(@NonNull final Writable output,
                                @NonNls @NonNull final String name,
                                @NonNull final V value) {
            mType.write(output, name, mConverter.from(value));
        }
    }

    private Types() {
        super();
    }
}
