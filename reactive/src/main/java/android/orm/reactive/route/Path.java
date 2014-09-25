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

package android.orm.reactive.route;

import android.content.ContentValues;
import android.net.Uri;
import android.orm.sql.Column;
import android.orm.sql.Readable;
import android.orm.sql.Select;
import android.orm.sql.Writable;
import android.orm.sql.fragment.Where;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static java.lang.System.arraycopy;

public class Path {

    /* package */ static final Path Root = new Path(
            Collections.<Segment>emptyList(),
            new SparseArray<Segment.Argument<?>>(0),
            "",
            Select.Projection.Nothing
    );

    @NonNls
    private static final MessageFormat WRONG_ARGUMENTS_ERROR = new MessageFormat("Wrong number of arguments (expected {0}, given {1})");
    @NonNls
    private static final String WRONG_PATH_ERROR = "Given uri path has wrong number of segments";
    private static final Object[] NO_ARGUMENTS = new Object[0];

    @NonNull
    private final List<Segment> mSegments;
    @NonNls
    @NonNull
    private final String mPath;
    @NonNull
    private final SparseArray<Segment.Argument<?>> mArguments;
    @NonNull
    private final Select.Projection mProjection;
    @NonNull
    private final Where.Builder<?>[] mWhere;

    private Path(@NonNull final List<Segment> segments,
                 @NonNull final SparseArray<Segment.Argument<?>> arguments,
                 @NonNls @NonNull final String path,
                 @NonNull final Select.Projection projection,
                 @NonNull final Where.Builder<?>... where) {
        super();

        mSegments = segments;
        mPath = path;
        mArguments = arguments;
        mProjection = projection;
        mWhere = where;
    }

    @NonNull
    public final Where getWhere(@NonNull final Uri uri) {
        return getWhere(parseArguments(uri));
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final Where getWhere(@NonNull final Object... arguments) {
        if (mWhere.length != arguments.length) {
            throw new IllegalArgumentException(WRONG_ARGUMENTS_ERROR.format(new Object[]{mWhere.length, arguments.length}));
        }

        Where where = Where.None;

        for (int i = 0; i < mWhere.length; i++) {
            where = where.and(((Where.Builder<Object>) mWhere[i]).build(arguments[i]));
        }

        return where;
    }

    @NonNull
    public final Select.Projection getProjection() {
        return mProjection;
    }

    @NonNull
    public final Path slash(@NonNls @NonNull final String literal) {
        return slash(new Segment.Literal(literal));
    }

    @NonNull
    public final <V> Path slash(@NonNull final Column<V> column) {
        return slash(Argument.isEqualTo(column));
    }

    @NonNull
    public final Path slash(@NonNull final Segment segment) {
        final List<Segment> segments = new ArrayList<>(mSegments.size() + 1);
        segments.addAll(mSegments);
        segments.add(segment);

        final String path = mSegments.isEmpty() ? segment.toString() : (mPath + '/' + segment);

        final SparseArray<Segment.Argument<?>> arguments;
        final Select.Projection projection;
        final Where.Builder<?>[] where;

        if (segment instanceof Segment.Argument) {
            final Segment.Argument<?> argument = (Segment.Argument<?>) segment;

            arguments = new SparseArray<>(mArguments.size() + 1);
            for (int i = 0; i < mArguments.size(); i++) {
                arguments.put(mArguments.keyAt(i), mArguments.valueAt(i));
            }
            arguments.put(mSegments.size(), argument);

            projection = mProjection.and(argument.getProjection());

            where = new Where.Builder<?>[mWhere.length + 1];
            arraycopy(mWhere, 0, where, 0, mWhere.length);
            where[mWhere.length] = argument.getWhere();
        } else {
            arguments = mArguments;
            projection = mProjection;
            where = mWhere;
        }

        return new Path(segments, arguments, path, projection, where);
    }

    @NonNull
    public final Maybe<String> createConcretePath(@NonNull final Readable input) {
        Maybe<String> result = null;

        final int size = mArguments.size();
        final Collection<Object> arguments = new ArrayList<>(size);
        for (int i = 0; (i < size) && (result == null); i++) {
            final Object value = mArguments.valueAt(i).read(input).getOrElse(null);
            if (value == null) {
                result = nothing();
            } else {
                arguments.add(value);
            }
        }

        return (result == null) ?
                something(createConcretePath(arguments.toArray(new Object[arguments.size()]))) :
                result;
    }

    @NonNull
    public final String createConcretePath(@NonNull final Object... arguments) {
        if (mArguments.size() != arguments.length) {
            throw new IllegalArgumentException(WRONG_ARGUMENTS_ERROR.format(new Object[]{mArguments.size(), arguments.length}));
        }

        final StringBuilder result = new StringBuilder();

        if (!mSegments.isEmpty()) {
            result.append(getUriPart(0, arguments));
            for (int i = 1; i < mSegments.size(); i++) {
                result.append('/').append(getUriPart(i, arguments));
            }
        }

        return result.toString();
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final ContentValues createValues(@NonNull final Object... arguments) {
        if (mArguments.size() != arguments.length) {
            throw new IllegalArgumentException(WRONG_ARGUMENTS_ERROR.format(new Object[]{mArguments.size(), arguments.length}));
        }

        final ContentValues result = new ContentValues();

        final int length = arguments.length;
        if (length > 0) {
            final Writable output = writable(result);
            for (int i = 0; i < length; i++) {
                ((Segment.Argument<Object>) mArguments.valueAt(i)).write(Insert, arguments[i], output);
            }
        }

        return result;
    }

    @NonNull
    public final ContentValues parseValues(@NonNull final Uri uri) {
        return createValues(parseArguments(uri));
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Path other = (Path) object;
            result = mPath.equals(other.mPath);
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return mPath.hashCode();
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return mPath;
    }

    @NonNull
    private Object[] parseArguments(@NonNull final Uri uri) {
        final List<String> segments = uri.getPathSegments();
        if ((segments == null) || (mSegments.size() != segments.size())) {
            throw new IllegalArgumentException(WRONG_PATH_ERROR);
        }

        final Object[] arguments;

        final int count = mArguments.size();
        if (count > 0) {
            arguments = new Object[count];
            for (int i = 0; i < count; i++) {
                arguments[i] = mArguments.valueAt(i).fromString(segments.get(mArguments.keyAt(i)));
            }
        } else {
            arguments = NO_ARGUMENTS;
        }

        return arguments;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private String getUriPart(final int index, @NonNull final Object... arguments) {
        final Segment segment = mSegments.get(index);
        return (segment instanceof Segment.Literal) ?
                segment.toString() :
                ((Segment.Argument<Object>) segment).toString(arguments[mArguments.indexOfKey(index)]);
    }
}
