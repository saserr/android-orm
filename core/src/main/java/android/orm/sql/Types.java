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
import android.orm.util.Legacy;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private static final String TAG = Type.class.getSimpleName();
    @NonNls
    private static final String FILE_NOT_FOUND = "Could not find file ";
    @NonNls
    private static final String FILE_NOT_CLOSED = "Could not close file ";

    public static final Type<String> Text = new Type.Base<String>(Type.Primitive.Text) {

        @NonNls
        @NonNull
        @Override
        public String fromString(@NonNls @NonNull final String value) {
            return value;
        }

        @NonNls
        @NonNull
        @Override
        public String toString(@NonNls @NonNull final String value) {
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

    public static final Type<Long> Integer = new Type.Base<Long>(Type.Primitive.Integer) {

        @NonNull
        @Override
        public Long fromString(@NonNls @NonNull final String value) {
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

    public static final Type<Double> Real = new Type.Base<Double>(Type.Primitive.Real) {

        @NonNull
        @Override
        public Double fromString(@NonNls @NonNull final String value) {
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

    public static final Type<Boolean> Bool = Integer.map(
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

    public static final Type<BigDecimal> Decimal = Text.map(
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

    public static final Type<File> File = Text.map(
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

    public static final Type<Uri> Uri = Text.map(
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

    public static final Type<Pair<File, byte[]>> Binary = File.map(
            new Converter<Pair<File, byte[]>, File>() {

                private static final int KILOBYTE = 1024;

                @NonNull
                @Override
                public File from(@NonNull final Pair<File, byte[]> pair) {
                    final File file = pair.first;
                    final byte[] bytes = pair.second;
                    final String path = file.getAbsolutePath();

                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(file);
                        out.write(bytes, 0, bytes.length);
                    } catch (final FileNotFoundException cause) {
                        final String message = FILE_NOT_FOUND + path;
                        Log.e(TAG, message, cause);
                        throw Legacy.wrap(message, cause);
                    } catch (final IOException cause) {
                        @NonNls final String message = "Could not write to file " + path;
                        Log.e(TAG, message, cause);
                        throw Legacy.wrap(message, cause);
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (final IOException cause) {
                                Log.w(TAG, FILE_NOT_CLOSED + path, cause);
                            }
                        }
                    }

                    return file;
                }

                @NonNull
                @Override
                public Pair<File, byte[]> to(@NonNull final File file) {
                    final String path = file.getAbsolutePath();
                    byte[] bytes = null;

                    ByteArrayOutputStream out = null;
                    InputStream in = null;
                    try {
                        in = new FileInputStream(file);
                        out = new ByteArrayOutputStream();

                        final byte[] buffer = new byte[KILOBYTE];
                        int read = in.read(buffer);
                        while (read != -1) {
                            out.write(buffer, 0, read);
                            read = in.read(buffer);
                        }

                        bytes = out.toByteArray();
                    } catch (final FileNotFoundException cause) {
                        @NonNls final String message = FILE_NOT_FOUND + path;
                        Log.e(TAG, message, cause);
                        throw Legacy.wrap(message, cause);
                    } catch (final IOException cause) {
                        @NonNls final String message = "Could not read from file " + path;
                        Log.e(TAG, message, cause);
                        throw Legacy.wrap(message, cause);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (final IOException cause) {
                                Log.w(TAG, FILE_NOT_CLOSED + path, cause);
                            }
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (final IOException cause) {
                                @NonNls final String message = "Byte stream not closed";
                                Log.w(TAG, message, cause);
                            }
                        }
                    }

                    return Pair.create(file, bytes);
                }
            }
    );

    @NonNull
    public static <E extends Enum<E>> Type<E> enumName(@NonNull final Class<E> klass) {
        return Text.map(new Converter<E, String>() {

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

    @NonNull
    public static Type<Pair<File, Bitmap>> bitmap(@NonNull final Bitmap.CompressFormat format,
                                                  final int quality,
                                                  @Nullable final BitmapFactory.Options options) {
        return File.map(
                new Converter<Pair<File, Bitmap>, File>() {

                    @NonNull
                    @Override
                    public File from(@NonNull final Pair<File, Bitmap> pair) {
                        final File file = pair.first;
                        final String path = file.getAbsolutePath();

                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(file);
                            pair.second.compress(format, quality, out);
                        } catch (final FileNotFoundException cause) {
                            @NonNls final String message = FILE_NOT_FOUND + path;
                            Log.e(TAG, message, cause);
                            throw Legacy.wrap(message, cause);
                        } finally {
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (final IOException cause) {
                                    Log.w(TAG, FILE_NOT_CLOSED + path, cause);
                                }
                            }
                        }

                        return file;
                    }

                    @NonNull
                    @Override
                    public Pair<File, Bitmap> to(@NonNull final File file) {
                        return Pair.create(file, BitmapFactory.decodeFile(file.getAbsolutePath(), options));
                    }
                }
        );
    }

    @NonNull
    public static <V, T> Type<V> map(@NonNull final Type<T> type,
                                     @NonNull final Converter<V, T> converter) {
        return new Conversion<>(type, converter);
    }

    private static class Conversion<V, T> extends Type.Base<V> {

        @NonNull
        private final Type<T> mType;
        @NonNull
        private final Converter<V, T> mConverter;

        private Conversion(@NonNull final Type<T> type, @NonNull final Converter<V, T> converter) {
            super(type.getPrimitive());

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
