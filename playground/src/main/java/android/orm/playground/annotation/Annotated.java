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

package android.orm.playground.annotation;

import android.orm.database.Migration;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.sql.Column;
import android.orm.sql.ForeignKey;
import android.orm.sql.PrimaryKey;
import android.orm.sql.Type;
import android.orm.sql.Value;
import android.orm.sql.Values;
import android.orm.sql.fragment.ConflictResolution;
import android.orm.util.Lens;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.orm.database.Migrations.create;
import static android.orm.database.Table.table;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.emptyList;

public class Annotated {

    public static final Annotated StandardTypesOnly = new Annotated(Types.withStandardTypes());

    @NonNull
    private final Types mTypes;

    public Annotated(@NonNull final Types types) {
        super();

        mTypes = types;
    }

    @NonNull
    public final <M> Mapper.ReadWrite<M> mapper(@NonNull final Class<M> klass) {
        return mapper(klass, new ReflectionProducer<>(klass));
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final <M> Mapper.ReadWrite<M> mapper(@NonNull final Class<M> klass,
                                                @NonNull final Producer<M> producer) {
        final String name = klass.getSimpleName();
        final Mapper.Read.Builder<M> read = Mapper.read(name, producer);
        final Mapper.Write.Builder<M> write = Mapper.write(name);

        for (final Field field : klass.getDeclaredFields()) {
            final int modifiers = field.getModifiers();
            if (!isStatic(modifiers)) {
                final Pair<android.orm.playground.annotation.Column, Column<Object>> column = column(klass, field);
                if (column != null) {
                    final boolean isFinal = isFinal(modifiers);
                    final boolean readOnly = column.first.readOnly();

                    if (isFinal && readOnly) {
                        @NonNls final String error = "Field " + field.getName() + " in " + name + " is final and read-only, " +
                                "which makes it non-readable and non-writable and thus irrelevant";
                        throw new IllegalArgumentException(error);
                    }

                    final Lens.ReadWrite<M, Object> lens = new ReflectionLens<>(name, field);
                    if (!isFinal) {
                        read.with(column.second, lens);
                    }
                    if (!readOnly) {
                        write.with(column.second, lens);
                    }
                }
            }
        }

        return Mappers.combine(read.build(), write.build());
    }

    @NonNull
    public final Migration migration(final int version, @NonNull final Class<?> klass) {
        final Table annotation = klass.getAnnotation(Table.class);
        if (annotation == null) {
            @NonNls final String error = "Class " + klass.getSimpleName() + " is not annotated with @Table";
            throw new IllegalArgumentException(error);
        }

        final String name = annotation.name();
        final PrimaryKey<?> primaryKey = primaryKey(klass);
        final ForeignKey<?>[] foreignKeys = foreignKeys(klass);
        final Column<?>[] columns = columns(klass);

        return (primaryKey == null) ?
                create(version, table(name, foreignKeys, columns)) :
                create(version, table(name, primaryKey, foreignKeys, columns));
    }

    @Nullable
    public final PrimaryKey<?> primaryKey(@NonNull final Class<?> klass) {
        PrimaryKey<?> result = null;

        final android.orm.playground.annotation.PrimaryKey annotation = klass.getAnnotation(android.orm.playground.annotation.PrimaryKey.class);
        if (annotation != null) {
            final Value.ReadWrite<?> value = value(annotation.columns());
            if (value == null) {
                @NonNls final String error = "@PrimaryKey must have non-empty columns " +
                        " (class " + klass.getSimpleName() + ')';
                throw new IllegalArgumentException(error);
            }

            final ConflictResolution resolution = annotation.resolution();
            result = (resolution == null) ?
                    PrimaryKey.primaryKey(value) :
                    PrimaryKey.primaryKey(value, resolution);
        }

        return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final ForeignKey<?>[] foreignKeys(@NonNull final Class<?> klass) {
        final ForeignKeys foreignKeys = klass.getAnnotation(ForeignKeys.class);
        final Collection<ForeignKey<?>> result;

        if (foreignKeys == null) {
            result = emptyList();
        } else {
            result = new ArrayList<>(foreignKeys.value().length);
            for (final android.orm.playground.annotation.ForeignKey annotation : foreignKeys.value()) {
                final Value.ReadWrite<Object> childKey = (Value.ReadWrite<Object>) value(annotation.childKey());
                if (childKey == null) {
                    @NonNls final String error = "@ForeignKey must have non-empty child key columns " +
                            " (class " + klass.getSimpleName() + ')';
                    throw new IllegalArgumentException(error);
                }
                final Value.ReadWrite<Object> parentKey = (Value.ReadWrite<Object>) value(annotation.parentKey());
                if (parentKey == null) {
                    @NonNls final String error = "@ForeignKey must have non-empty parent key columns " +
                            " (class " + klass.getSimpleName() + ')';
                    throw new IllegalArgumentException(error);
                }

                final Class<?> parent = annotation.parent();
                final Table table = parent.getAnnotation(Table.class);
                if (table == null) {
                    @NonNls final String error = "@ForeignKey must point to parent class that is annotated with @Table " +
                            " (class " + klass.getSimpleName() + ", parent " + parent.getSimpleName() + ')';
                    throw new IllegalArgumentException(error);
                }
                result.add(ForeignKey.foreignKey(childKey, table.name(), parentKey)
                        .onDelete(annotation.onDelete())
                        .onUpdate(annotation.onUpdate()));
            }
        }

        return result.toArray(new ForeignKey[result.size()]);
    }

    @NonNull
    public final Column<?>[] columns(@NonNull final Class<?> klass) {
        final List<Column<?>> result = new ArrayList<>();

        for (final Field field : klass.getDeclaredFields()) {
            if (!isStatic(field.getModifiers())) {
                final Pair<android.orm.playground.annotation.Column, Column<Object>> column = column(klass, field);
                if (column != null) {
                    result.add(column.second);
                }
            }
        }

        return result.toArray(new Column[result.size()]);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <M, V> Type<V> type(@NonNull final Class<M> klass, @NonNull final Field field) {
        final Type<V> type = (Type<V>) mTypes.get(field.getType());

        if (type == null) {
            @NonNls final String error = "Unknown SQL type for " + field.getType().getSimpleName() +
                    " (field " + field.getName() + " in " + klass.getSimpleName() + ')';
            throw new IllegalArgumentException(error);
        }

        return type;
    }

    @Nullable
    private <M, V> Pair<android.orm.playground.annotation.Column, Column<V>> column(@NonNull final Class<M> klass,
                                                                                    @NonNull final Field field) {
        @org.jetbrains.annotations.Nullable final Pair<android.orm.playground.annotation.Column, Column<V>> result;

        final android.orm.playground.annotation.Column annotation = field.getAnnotation(android.orm.playground.annotation.Column.class);
        if (annotation == null) {
            result = null;
        } else {
            Column<V> column = Column.column(annotation.name(), this.<M, V>type(klass, field));
            if (annotation.unique()) {
                column = column.asUnique();
            }
            if (!annotation.nullable()) {
                column = column.asNotNull();
            }
            result = Pair.create(annotation, column);
        }

        return result;
    }

    @Nullable
    private static Value.ReadWrite<?> value(@NonNull final String... columns) {
        Value.ReadWrite<?> result = null;

        for (final String column : columns) {
            final Value.ReadWrite<Object> value = Values.value(column, null);
            result = (result == null) ? value : result.and(value);
        }

        return result;
    }

    private static class ReflectionProducer<M> implements Producer<M> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Constructor<M> mConstructor;

        private ReflectionProducer(@NonNull final Class<M> klass) {
            super();


            mName = klass.getSimpleName();

            Constructor<M> constructor;
            try {
                constructor = klass.getDeclaredConstructor();
            } catch (final NoSuchMethodException ignored) {
                try {
                    constructor = klass.getConstructor();
                } catch (final NoSuchMethodException ex) {
                    throw new IllegalArgumentException("No-argument constructor in " + mName + " is missing", ex); //NON-NLS
                }
            }
            mConstructor = constructor;
        }

        @NonNull
        @Override
        public final M produce() {
            if (!mConstructor.isAccessible()) {
                mConstructor.setAccessible(true);
            }

            final M result;

            try {
                result = mConstructor.newInstance();
            } catch (final InstantiationException ex) {
                throw new UnsupportedOperationException("Error creating instance of " + mName, ex); //NON-NLS
            } catch (final IllegalAccessException ex) {
                throw new UnsupportedOperationException("No-argument constructor in " + mName + " is not accessible", ex); //NON-NLS
            } catch (final InvocationTargetException ex) {
                throw new UnsupportedOperationException("Error creating instance of " + mName, ex.getTargetException()); //NON-NLS
            }

            return result;
        }
    }

    private static class ReflectionLens<M, V> implements Lens.ReadWrite<M, V> {

        @NonNull
        private final Field mField;
        @NonNls
        @NonNull
        private final String mNotAccessible;

        private ReflectionLens(@NonNls @NonNull final String name, @NonNull final Field field) {
            super();

            mField = field;
            mNotAccessible = "Field " + field.getName() + " in " + name + " is not accessible";
        }

        @Override
        public final void set(@NonNull final M model, @Nullable final V value) {
            if (!mField.isAccessible()) {
                mField.setAccessible(true);
            }

            try {
                mField.set(model, value);
            } catch (final IllegalAccessException ex) {
                throw new UnsupportedOperationException(mNotAccessible, ex); //NON-NLS
            }
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public final V get(@NonNull final M model) {
            if (!mField.isAccessible()) {
                mField.setAccessible(true);
            }

            final V value;

            try {
                value = (V) mField.get(model);
            } catch (final IllegalAccessException ex) {
                throw new UnsupportedOperationException(mNotAccessible, ex); //NON-NLS
            }

            return value;
        }
    }
}

