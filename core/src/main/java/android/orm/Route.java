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

package android.orm;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.net.Uri;
import android.orm.route.Match;
import android.orm.route.Path;
import android.orm.sql.Column;
import android.orm.sql.Readable;
import android.orm.sql.Table;
import android.orm.sql.statement.Select;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static android.orm.route.Paths.path;

public abstract class Route {

    @NonNls
    private static final MessageFormat CONTENT_TYPE_FORMAT = new MessageFormat("vnd.android.cursor.{0}/{1}.{2}");
    @NonNls
    private static final MessageFormat URI_FORMAT = new MessageFormat("content://{0}/{1}");

    @NonNull
    private final Manager mManager;
    @NonNull
    private final Table mTable;
    @NonNull
    private final Select.Where mWhere;
    @Nullable
    private final Select.Order mOrder;
    @NonNull
    private final Path mPath;
    @NonNls
    @NonNull
    private final String mAuthority;
    @NonNls
    @NonNull
    private final String mContentType;
    @NonNull
    private final Select.Projection mProjection;

    protected Route(@NonNull final Manager manager,
                    @Type @NonNls @NonNull final String type,
                    @NonNull final Table table,
                    @NonNull final Select.Where where,
                    @Nullable final Select.Order order,
                    @NonNull final Path path) {
        super();

        mManager = manager;
        mTable = table;
        mWhere = where;
        mOrder = order;
        mPath = path;

        mAuthority = manager.getAuthority();
        mContentType = getContentType(type, manager.getContentTypePrefix(), table);
        mProjection = path.getProjection();
    }

    protected abstract Item getItemRoute();

    @NonNull
    public final Manager getManager() {
        return mManager;
    }

    @NonNls
    @NonNull
    public final Path getPath() {
        return mPath;
    }

    @NonNull
    public final Select.Projection getProjection() {
        return mProjection;
    }

    @NonNull
    public final Uri createUri(@NonNull final Readable input) {
        return Uri.parse(URI_FORMAT.format(new String[]{mAuthority, mPath.createConcretePath(input)}));
    }

    @NonNull
    public final Uri createUri(@NonNull final Object... arguments) {
        return Uri.parse(URI_FORMAT.format(new String[]{mAuthority, mPath.createConcretePath(arguments)}));
    }

    @NonNull
    public final Match createMatch(@NonNull final Uri uri) {
        final Select.Where where = mPath.getWhere(uri);
        return new Match(getItemRoute(), mContentType, mTable, parse(uri), mWhere.and(where), mOrder);
    }

    public final ContentValues parse(@NonNull final Uri uri) {
        return mPath.parseValues(uri);
    }

    private static String getContentType(@Type @NonNls @NonNull final String type,
                                         @NonNull final String prefix,
                                         @NonNull final Table table) {
        return CONTENT_TYPE_FORMAT.format(new String[]{type, prefix, table.getName()});
    }

    public static class Dir extends Route {

        @NonNull
        private final Item mItemRoute;

        public Dir(@NonNull final Manager manager,
                   @NonNull final Item itemRoute,
                   @NonNull final Table table,
                   @NonNull final Select.Order order,
                   @NonNull final Select.Where where,
                   @NonNull final Path path) {
            super(manager, Type.Dir, table, where, order, path);

            mItemRoute = itemRoute;
        }

        @Override
        protected final Item getItemRoute() {
            return mItemRoute;
        }
    }

    public static class Item extends Route {

        @NonNull
        private final Item mItemRoute;

        public Item(@NonNull final Manager manager,
                    @NonNull final Table table,
                    @NonNull final Select.Where where,
                    @NonNull final Path path) {
            super(manager, Type.Item, table, where, null, path);

            mItemRoute = this;
        }

        public Item(@NonNull final Manager manager,
                    @NonNull final Item itemRoute,
                    @NonNull final Table table,
                    @NonNull final Select.Where where,
                    @NonNull final Path path) {
            super(manager, Type.Item, table, where, null, path);

            mItemRoute = itemRoute;
        }

        @Override
        protected final Item getItemRoute() {
            return mItemRoute;
        }
    }

    public static class Manager {

        @NonNls
        @NonNull
        private final String mAuthority;
        @NonNls
        @NonNull
        private final String mContentTypePrefix;

        private final ReadWriteLock mLock = new ReentrantReadWriteLock();
        private final Collection<Path> mPaths = new HashSet<>();
        private final List<Route> mRoutes = new ArrayList<>();
        private final UriMatcher mMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        public Manager(@NonNls @NonNull final String authority,
                       @NonNls @NonNull final String contentTypePrefix) {
            super();

            mAuthority = authority;
            mContentTypePrefix = contentTypePrefix;
        }

        @NonNls
        @NonNull
        public final String getAuthority() {
            return mAuthority;
        }

        @NonNls
        @NonNull
        public final String getContentTypePrefix() {
            return mContentTypePrefix;
        }

        @NonNull
        public final Dir dir(@NonNull final Item itemRoute,
                             @NonNull final Table table) {
            return dir(itemRoute, table, path(table.getName()));
        }

        @NonNull
        public final Dir dir(@NonNull final Item itemRoute,
                             @NonNull final Column.Reference reference,
                             @NonNull final Table table) {
            final Path path = path(reference.getTable().getName())
                    .slash(reference)
                    .slash(table.getName());
            return dir(itemRoute, table, path);
        }

        @NonNull
        public final Dir dir(@NonNull final Item itemRoute,
                             @NonNull final Table table,
                             @NonNull final Path path) {
            return dir(itemRoute, table, Select.Where.None, path);
        }

        @NonNull
        public final Dir dir(@NonNull final Item itemRoute,
                             @NonNull final Table table,
                             @NonNull final Select.Where where,
                             @NonNull final Path path) {
            return dir(itemRoute, table, where, table.getOrder(), path);
        }

        @NonNull
        public final Dir dir(@NonNull final Item itemRoute,
                             @NonNull final Table table,
                             @NonNull final Select.Order order,
                             @NonNull final Path path) {
            return dir(itemRoute, table, Select.Where.None, order, path);
        }

        @NonNull
        public final Dir dir(@NonNull final Item itemRoute,
                             @NonNull final Table table,
                             @NonNull final Select.Where where,
                             @NonNull final Select.Order order,
                             @NonNull final Path path) {
            final Dir route = new Dir(this, itemRoute, table, order, where, path);
            with(route);
            return route;
        }

        @NonNull
        public final Item item(@NonNull final Table table) {
            return item(table, path(table.getName()).slash(table.getPrimaryKey()));
        }

        @NonNull
        public final Item item(@NonNull final Column.Reference reference,
                               @NonNull final Table table) {
            Path path = path(reference.getTable().getName())
                    .slash(reference)
                    .slash(table.getName());

            if (!reference.isUnique()) {
                path = path.slash(table.getPrimaryKey());
            }

            return item(table, path);
        }

        @NonNull
        public final Item item(@NonNull final Table table, @NonNull final Path path) {
            return item(table, Select.Where.None, path);
        }

        @NonNull
        public final Item item(@NonNull final Table table,
                               @NonNull final Select.Where where,
                               @NonNull final Path path) {
            final Item route = new Item(this, table, where, path);
            with(route);
            return route;
        }

        @NonNull
        public final <V> Item item(@NonNull final Item itemRoute,
                                   @NonNull final Table table,
                                   @NonNull final Column<V> column) {
            if (!column.isUnique()) {
                throw new IllegalArgumentException("Column must be unique");
            }

            final Path path = path(table.getName()).slash(column.getName()).slash(column);
            return item(itemRoute, table, path);
        }

        @NonNull
        public final Item item(@NonNull final Item itemRoute,
                               @NonNull final Table table,
                               @NonNull final Path path) {
            return item(itemRoute, table, Select.Where.None, path);
        }

        @NonNull
        public final Item item(@NonNull final Item itemRoute,
                               @NonNull final Table table,
                               @NonNull final Select.Where where,
                               @NonNull final Path path) {
            final Item route = new Item(this, itemRoute, table, where, path);
            with(route);
            return route;
        }

        @Nullable
        public final Match match(@NonNull final Uri uri) {
            final int code = mMatcher.match(uri);
            Match result = null;

            if (code >= 0) {
                mLock.readLock().lock();
                try {
                    result = mRoutes.get(code).createMatch(uri);
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

    @Retention(RetentionPolicy.CLASS)
    @StringDef({Type.Dir, Type.Item})
    public @interface Type {
        @NonNls
        String Dir = "dir";
        @NonNls
        String Item = "item";
    }
}
