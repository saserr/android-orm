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

package android.orm.remote;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.net.Uri;
import android.orm.remote.route.Path;
import android.orm.sql.Column;
import android.orm.sql.ForeignKey;
import android.orm.sql.Readable;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static android.orm.remote.route.Paths.path;

public abstract class Route extends Value.Read.Base<Uri> {

    @NonNls
    private static final MessageFormat URI_FORMAT = new MessageFormat("content://{0}/{1}");

    @NonNull
    private final Manager mManager;
    @NonNls
    @NonNull
    private final String mTable;
    @NonNull
    private final Where mWhere;
    @NonNull
    private final Path mPath;
    @NonNls
    @NonNull
    private final String mAuthority;
    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Select.Projection mProjection;
    @Nullable
    private final Order mOrder;
    @Nullable
    private final Limit mLimit;

    private final Function<String, Uri> mUriFormat = new Function<String, Uri>() {
        @NonNull
        @Override
        public Uri invoke(@NonNull final String path) {
            return Uri.parse(URI_FORMAT.format(new String[]{mAuthority, path}));
        }
    };

    private Route(@NonNull final Manager manager,
                  @NonNls @NonNull final String table,
                  @NonNull final Where where,
                  @Nullable final Order order,
                  @Nullable final Limit limit,
                  @NonNull final Path path) {
        super();

        mManager = manager;
        mTable = table;
        mWhere = where;
        mOrder = order;
        mLimit = limit;
        mPath = path;

        mAuthority = manager.getAuthority();
        mName = URI_FORMAT.format(new String[]{mAuthority, path.toString()});
        mProjection = path.getProjection();
    }

    @NonNull
    public abstract Single getSingleRoute();

    @NonNull
    public final Manager getManager() {
        return mManager;
    }

    @NonNls
    @NonNull
    public final String getTable() {
        return mTable;
    }

    @Nullable
    public final Order getOrder() {
        return mOrder;
    }

    @Nullable
    public final Limit getLimit() {
        return mLimit;
    }

    @NonNls
    @NonNull
    public final Path getPath() {
        return mPath;
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    public final Select.Projection getProjection() {
        return mProjection;
    }

    @NonNull
    @Override
    public final Maybe<Uri> read(@NonNull final Readable input) {
        return mPath.createConcretePath(input).map(mUriFormat);
    }

    @NonNull
    public final Where createWhere(@NonNull final Object... arguments) {
        return mWhere.and(mPath.getWhere(arguments));
    }

    @NonNull
    public final ContentValues createValues(@NonNull final Object... arguments) {
        return mPath.createValues(arguments);
    }

    @NonNull
    public final Uri createUri(@NonNull final Object... arguments) {
        return mUriFormat.invoke(mPath.createConcretePath(arguments));
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return mName;
    }

    public static class Many extends Route {

        @NonNull
        private final Single mSingleRoute;

        public Many(@NonNull final Manager manager,
                    @NonNull final Single singleRoute,
                    @NonNull final Where where,
                    @Nullable final Order order,
                    @Nullable final Limit limit,
                    @NonNull final Path path) {
            super(manager, singleRoute.getTable(), where, order, limit, path);

            mSingleRoute = singleRoute;
        }

        @NonNull
        @Override
        public final Single getSingleRoute() {
            return mSingleRoute;
        }
    }

    public static class Single extends Route {

        @NonNull
        private final Single mSingleRoute;

        public Single(@NonNull final Manager manager,
                      @NonNls @NonNull final String table,
                      @NonNull final Where where,
                      @NonNull final Path path) {
            super(manager, table, where, null, Limit.Single, path);

            mSingleRoute = this;
        }

        public Single(@NonNull final Manager manager,
                      @NonNull final Single singleRoute,
                      @NonNull final Where where,
                      @NonNull final Path path) {
            super(manager, singleRoute.getTable(), where, singleRoute.getOrder(), Limit.Single, path);

            mSingleRoute = singleRoute;
        }

        @NonNull
        @Override
        public final Single getSingleRoute() {
            return mSingleRoute;
        }
    }

    public static class Manager {

        @NonNls
        @NonNull
        private final String mAuthority;

        private final ReadWriteLock mLock = new ReentrantReadWriteLock();
        private final Collection<Path> mPaths = new HashSet<>();
        private final List<Route> mRoutes = new ArrayList<>();
        private final UriMatcher mMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        public Manager(@NonNls @NonNull final String authority) {
            super();

            mAuthority = authority;
        }

        @NonNls
        @NonNull
        public final String getAuthority() {
            return mAuthority;
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute) {
            return many(singleRoute, null, (Limit) null);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute, @NonNull final Order order) {
            return many(singleRoute, order, (Limit) null);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Limit limit) {
            return many(singleRoute, null, limit);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @Nullable final Order order,
                               @Nullable final Limit limit) {
            return many(singleRoute, order, limit, path(singleRoute.getTable()));
        }

        @NonNull
        public final <R> Many many(@NonNull final Single singleRoute,
                                   @NonNull final ForeignKey<R> foreignKey) {
            return many(singleRoute, null, null, foreignKey);
        }

        @NonNull
        public final <R> Many many(@NonNull final Single singleRoute,
                                   @NonNull final Order order,
                                   @NonNull final ForeignKey<R> foreignKey) {
            return many(singleRoute, order, null, foreignKey);
        }

        @NonNull
        public final <R> Many many(@NonNull final Single singleRoute,
                                   @NonNull final Limit limit,
                                   @NonNull final ForeignKey<R> foreignKey) {
            return many(singleRoute, null, limit, foreignKey);
        }

        @NonNull
        public final <R> Many many(@NonNull final Single singleRoute,
                                   @Nullable final Order order,
                                   @Nullable final Limit limit,
                                   @NonNull final ForeignKey<R> foreignKey) {
            final Value.Read<R> child = foreignKey.getChildKey();
            if (!(child instanceof Column)) {
                throw new IllegalArgumentException("Reference must be a single column");
            }
            final Column<R> reference = (Column<R>) child;

            final Path path = path(foreignKey.getParentTable())
                    .slash(reference)
                    .slash(singleRoute.getTable());
            return many(singleRoute, order, limit, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Path path) {
            return many(singleRoute, (Order) null, null, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Order order,
                               @NonNull final Path path) {
            return many(singleRoute, order, null, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Limit limit,
                               @NonNull final Path path) {
            return many(singleRoute, (Order) null, limit, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @Nullable final Order order,
                               @Nullable final Limit limit,
                               @NonNull final Path path) {
            return many(singleRoute, Where.None, order, limit, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Where where,
                               @NonNull final Path path) {
            return many(singleRoute, where, null, null, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Where where,
                               @NonNull final Order order,
                               @NonNull final Path path) {
            return many(singleRoute, where, order, null, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Where where,
                               @NonNull final Limit limit,
                               @NonNull final Path path) {
            return many(singleRoute, where, null, limit, path);
        }

        @NonNull
        public final Many many(@NonNull final Single singleRoute,
                               @NonNull final Where where,
                               @Nullable final Order order,
                               @Nullable final Limit limit,
                               @NonNull final Path path) {
            final Many route = new Many(this, singleRoute, where, order, limit, path);
            with(route);
            return route;
        }

        @NonNull
        public final <R> Single single(@NonNull final ForeignKey<R> foreignKey,
                                       @NonNls @NonNull final String table) {
            final Value.Read<R> child = foreignKey.getChildKey();
            if (!(child instanceof Column)) {
                throw new IllegalArgumentException("Foreign key must be a single column");
            }
            final Column<R> reference = (Column<R>) child;
            if (!reference.isUnique()) {
                throw new IllegalArgumentException("Foreign key must be unique");
            }

            final Path path = path(foreignKey.getParentTable())
                    .slash(reference)
                    .slash(table);
            return single(table, path);
        }

        @NonNull
        public final <R, V> Single single(@NonNull final ForeignKey<R> foreignKey,
                                          @NonNls @NonNull final String table,
                                          @NonNull final Column<V> column) {
            final Value.Read<R> child = foreignKey.getChildKey();
            if (!(child instanceof Column)) {
                throw new IllegalArgumentException("Foreign key must be a single column");
            }
            final Column<R> reference = (Column<R>) child;

            final Single route;

            if (reference.isUnique()) {
                route = single(foreignKey, table);
            } else {
                if (!column.isUnique()) {
                    throw new IllegalArgumentException("Column must be unique");
                }

                final Path path = path(foreignKey.getParentTable())
                        .slash(reference)
                        .slash(table)
                        .slash(column);

                route = single(table, path);
            }

            return route;
        }

        @NonNull
        public final <V> Single single(@NonNls @NonNull final String table,
                                       @NonNull final Column<V> column) {
            if (!column.isUnique()) {
                throw new IllegalArgumentException("Column must be unique");
            }

            return single(table, path(table).slash(column.getName()).slash(column));
        }

        @NonNull
        public final Single single(@NonNls @NonNull final String table, @NonNull final Path path) {
            return single(table, Where.None, path);
        }

        @NonNull
        public final Single single(@NonNls @NonNull final String table,
                                   @NonNull final Where where,
                                   @NonNull final Path path) {
            final Single route = new Single(this, table, where, path);
            with(route);
            return route;
        }

        @NonNull
        public final <V> Single single(@NonNull final Single singleRoute,
                                       @NonNull final Column<V> column) {
            if (!column.isUnique()) {
                throw new IllegalArgumentException("Column must be unique");
            }

            final Path path = path(singleRoute.getTable()).slash(column.getName()).slash(column);
            return single(singleRoute, path);
        }

        @NonNull
        public final Single single(@NonNull final Single singleRoute,
                                   @NonNull final Path path) {
            return single(singleRoute, Where.None, path);
        }

        @NonNull
        public final Single single(@NonNull final Single singleRoute,
                                   @NonNull final Where where,
                                   @NonNull final Path path) {
            final Single route = new Single(this, singleRoute, where, path);
            with(route);
            return route;
        }

        @Nullable
        public final Route get(@NonNull final Uri uri) {
            final int code = mMatcher.match(uri);
            Route result = null;

            if (code >= 0) {
                mLock.readLock().lock();
                try {
                    result = mRoutes.get(code);
                } finally {
                    mLock.readLock().unlock();
                }
            }

            return result;
        }

        private void with(@NonNull final Route route) {
            final Path path = route.getPath();

            mLock.writeLock().lock();
            try {
                if (mPaths.contains(path)) {
                    throw new IllegalArgumentException("A route with same path " + path + " has already been added!");
                }

                mMatcher.addURI(mAuthority, path.toString(), mRoutes.size());
                mPaths.add(path);
                mRoutes.add(route);
            } finally {
                mLock.writeLock().unlock();
            }
        }
    }
}
