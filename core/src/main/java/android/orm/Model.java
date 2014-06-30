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

import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Plans;
import android.orm.model.Property;
import android.orm.model.Reading;
import android.orm.model.Storage;
import android.orm.model.View;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Converter;
import android.orm.util.Converters;
import android.orm.util.Function;
import android.orm.util.Producer;
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;

public abstract class Model {

    private final Collection<Instance.Readable> mReadableInstances = new ArrayList<>();
    private final Collection<Instance.Writable> mWritableInstances = new ArrayList<>();

    @NonNull
    private final Instance.ReadWrite mInstance;

    protected Model() {
        this(null, Observer.ReadWrite.DUMMY);
    }

    protected Model(@NonNls @NonNull final String name) {
        this(name, Observer.ReadWrite.DUMMY);
    }

    protected Model(@NonNull final Observer.ReadWrite observer) {
        this(null, observer);
    }

    protected Model(@NonNls @Nullable final String name,
                    @NonNull final Observer.ReadWrite observer) {
        super();

        mInstance = new ModelInstance<>(name, this, observer);
    }

    protected final void with(@NonNull final Instance.Readable instance) {
        mReadableInstances.add(instance);
    }

    protected final void with(@NonNull final Instance.Writable instance) {
        mWritableInstances.add(instance);
    }

    protected final void with(@NonNull final Instance.ReadWrite instance) {
        with((Instance.Readable) instance);
        with((Instance.Writable) instance);
    }

    @NonNull
    protected final <V> View<V> view(@NonNull final Value.Read<V> value) {
        final View<V> view = new View<>(value, null);
        with(view);
        return view;
    }

    @NonNull
    protected final <V> View<V> view(@NonNull final Mapper.Read<V> mapper) {
        final View<V> view = new View<>(mapper, null);
        with(view);
        return view;
    }

    @NonNull
    protected final <V> View<V> view(@NonNull final Value.Read<V> value,
                                     @NonNull final Observer.Read observer) {
        final View<V> view = new View<>(value, observer);
        with(view);
        return view;
    }

    @NonNull
    protected final <V> View<V> view(@NonNull final Mapper.Read<V> mapper,
                                     @NonNull final Observer.Read observer) {
        final View<V> view = new View<>(mapper, observer);
        with(view);
        return view;
    }

    protected final void storage(@NonNull final Writer writer) {
        with(new Storage.Constant(writer));
    }

    @NonNull
    protected final <V> Storage<V> storage(@NonNull final Value.Write<V> value) {
        final Storage<V> storage = new Storage<>(value, null);
        with(storage);
        return storage;
    }

    @NonNull
    protected final <V> Storage<V> storage(@NonNull final Mapper.Write<V> mapper) {
        final Storage<V> storage = new Storage<>(mapper, null);
        with(storage);
        return storage;
    }

    @NonNull
    protected final <V> Storage<V> storage(@NonNull final Value.Write<V> value,
                                           @NonNull final Observer.Write observer) {
        final Storage<V> storage = new Storage<>(value, observer);
        with(storage);
        return storage;
    }

    @NonNull
    protected final <V> Storage<V> storage(@NonNull final Mapper.Write<V> mapper,
                                           @NonNull final Observer.Write observer) {
        final Storage<V> storage = new Storage<>(mapper, observer);
        with(storage);
        return storage;
    }

    @NonNull
    protected final <V> Property<V> property(@NonNull final Value.ReadWrite<V> value) {
        final Property<V> property = new Property<>(value, null);
        with(property);
        return property;
    }

    @NonNull
    protected final <V> Property<V> property(@NonNull final Mapper.ReadWrite<V> mapper) {
        final Property<V> property = new Property<>(mapper, null);
        with(property);
        return property;
    }

    @NonNull
    protected final <V> Property<V> property(@NonNull final Value.ReadWrite<V> value,
                                             @NonNull final Observer.ReadWrite observer) {
        final Property<V> property = new Property<>(value, observer);
        with(property);
        return property;
    }

    @NonNull
    protected final <V> Property<V> property(@NonNull final Mapper.ReadWrite<V> mapper,
                                             @NonNull final Observer.ReadWrite observer) {
        final Property<V> property = new Property<>(mapper, observer);
        with(property);
        return property;
    }

    @NonNull
    private static Collection<Instance.Readable> getReadableInstances(@NonNull final Model model) {
        return model.mReadableInstances;
    }

    @NonNull
    private static Collection<Instance.Writable> getWritableInstances(@NonNull final Model model) {
        return model.mWritableInstances;
    }

    @NonNull
    public static Instance.ReadWrite toInstance(@NonNull final Model model) {
        return model.mInstance;
    }

    private static class ModelInstance<M extends Model> implements Instance.ReadWrite, Observer.ReadWrite {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final M mModel;
        @NonNull
        private final Observer.ReadWrite mObserver;

        private ModelInstance(@NonNls @Nullable final String name,
                              @NonNull final M model,
                              @NonNull final Observer.ReadWrite observer) {
            super();

            mName = (name == null) ? model.getClass().getSimpleName() : name;
            mModel = model;
            mObserver = observer;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        public final M getModel() {
            return mModel;
        }

        @NonNull
        @Override
        public final Reading.Item.Action prepareRead() {
            final Collection<Instance.Readable> instances = getReadableInstances(mModel);
            final Collection<Reading.Item.Action> actions = new ArrayList<>(instances.size());

            for (final Instance.Readable instance : instances) {
                actions.add(instance.prepareRead());
            }

            return Reading.Item.compose(actions);
        }

        @NonNull
        @Override
        public final Plan.Write prepareWrite() {
            final Collection<Instance.Writable> instances = getWritableInstances(mModel);
            final Collection<Plan.Write> plans = new ArrayList<>(instances.size());

            for (final Instance.Writable instance : instances) {
                plans.add(instance.prepareWrite());
            }

            return Plans.compose(plans);
        }

        @Override
        public final void beforeRead() {
            for (final Instance.Readable instance : getReadableInstances(mModel)) {
                if (instance instanceof Observer.Read) {
                    ((Observer.Read) instance).beforeRead();
                }
            }
            mObserver.beforeRead();
        }

        @Override
        public final void afterRead() {
            for (final Instance.Readable instance : getReadableInstances(mModel)) {
                if (instance instanceof Observer.Read) {
                    ((Observer.Read) instance).afterRead();
                }
            }
            mObserver.afterRead();
        }

        @Override
        public final void beforeCreate() {
            for (final Instance.Writable instance : getWritableInstances(mModel)) {
                if (instance instanceof Observer.Write) {
                    ((Observer.Write) instance).beforeCreate();
                }
            }
            mObserver.beforeCreate();
        }

        @Override
        public final void afterCreate() {
            for (final Instance.Writable instance : getWritableInstances(mModel)) {
                if (instance instanceof Observer.Write) {
                    ((Observer.Write) instance).afterCreate();
                }
            }
            mObserver.afterCreate();
        }

        @Override
        public final void beforeUpdate() {
            for (final Instance.Writable instance : getWritableInstances(mModel)) {
                if (instance instanceof Observer.Write) {
                    ((Observer.Write) instance).beforeUpdate();
                }
            }
            mObserver.beforeUpdate();
        }

        @Override
        public final void afterUpdate() {
            for (final Instance.Writable instance : getWritableInstances(mModel)) {
                if (instance instanceof Observer.Write) {
                    ((Observer.Write) instance).afterUpdate();
                }
            }
            mObserver.afterUpdate();
        }

        @Override
        public final void beforeSave() {
            for (final Instance.Writable instance : getWritableInstances(mModel)) {
                if (instance instanceof Observer.Write) {
                    ((Observer.Write) instance).beforeSave();
                }
            }
            mObserver.beforeSave();
        }

        @Override
        public final void afterSave() {
            for (final Instance.Writable instance : getWritableInstances(mModel)) {
                if (instance instanceof Observer.Write) {
                    ((Observer.Write) instance).afterSave();
                }
            }
            mObserver.afterSave();
        }
    }

    private static final Converter<ModelInstance<Model>, Model> ModelInstanceConverter =
            new Converter<ModelInstance<Model>, Model>() {

                @NonNull
                @Override
                public Model from(@NonNull final ModelInstance<Model> instance) {
                    return instance.getModel();
                }

                @NonNull
                @Override
                @SuppressWarnings("unchecked")
                public ModelInstance<Model> to(@NonNull final Model model) {
                    return (ModelInstance<Model>) toInstance(model);
                }
            };
    private static final Function<Model, ModelInstance<Model>> ToInstance = Converters.to(ModelInstanceConverter);

    @NonNull
    @SuppressWarnings("unchecked")
    public static <M extends Model> Mapper.ReadWrite<M> mapper(@NonNull final Producer<M> producer) {
        final Function<M, ModelInstance<M>> toInstance = (Function<M, ModelInstance<M>>) (Object) ToInstance;
        final Converter<ModelInstance<M>, M> converter = (Converter<ModelInstance<M>, M>) (Object) ModelInstanceConverter;
        return Mappers.mapper(Producers.convert(producer, toInstance)).map(converter);
    }
}
