# Android ORM

This ORM is not yet another (annotiation-based) ORM library; it is my take on how an ORM should work. In my opinion, the problem with most other ORM libraries is that they always mix mapping logic (a logic that specifies how value is stored and retrieved from a database) with actual object that store information (e.g. annotated JavaBeans). Hence, they require developers to create specialized objects that can be stored and retrieved from the database and only this type of objects can be used in such manner.

On the other hand, the idea behind this ORM library is to separate objects that store information from the actual mapping logic. This allows us to use any object like String, JavaBeans, a 3rd-party library class, or even a Json object as a source of information that can be stored or retrieved. Of course, the mapping logic still needs to be specified by the developer before some object can be used to store and retrieve information from a database and this library provides many different ways for a developer to easily specify such mapping logics. An example of this concept where 3rd-party class LatLng from google play services is stored and retrieved from a table addresses is shown in the following snippet:

```java
import java.util.List;

import com.google.android.gms.maps.model.LatLng;

import android.orm.DAO;
import android.orm.Database;
import android.orm.sql.Types;
import android.orm.sql.Value;
import android.orm.util.Consumer;
import android.orm.util.Converter;
import android.util.Pair;

import static android.orm.DAO.byRowId;
import static android.orm.sql.Values.value;

// create mappings for latitude and longitude columns
Value.ReadWrite<Double> latitude = value("latitude", Types.Real);
Value.ReadWrite<Double> longitude = value("longitude", Types.Real);
// create a new mapping for 3rd-party class LatLng in google play services
// out of latitude and longitude mappings
Value.ReadWrite<LatLng> latLng = latitude.and(longitude)
    .map(new Converter<Pair<Double, Double>, LatLng>() {

      @NonNull
      @Override
      public LatLng from(@NonNull Pair<Double, Double> pair) {
        return new LatLng(pair.first, pair.second);
      }

      @NonNull
      @Override
      public Pair<Double, Double> to(@NonNull LatLng latLng) {
        return Pair.create(latLng.latitude, latLng.longitude);
      }
    });

// create an asynchronous DAO to access the database
Database database = new Database("data.db", 1);
DAO.Async dao = DAO.create(getApplicationContext(), database);

// create an Access for table addresses
Access.Async.Many<Long> addresses = dao.access(byRowId("addresses"));

// asynchronously read all LatLng in table addresses
addresses.query().select(latLng)
    .onSomething(new Consumer<List<LatLng>>() {
      @Override
      public void consume(@Nullable List<LatLng> values) {
        // do something
      }
    });

// asynchronously update LatLng for address with row id 1
dao.access(byRowId("addresses", 1)).update(new LatLng(0, 0), latLng);
```

Beside `Value.ReadWrite` that was shown in the previous snippet, this library provides further 11 ways to specify mapping logics and they are:
* `Value`, `Value.Read`, `Value.Write`, and `Value.ReadWrite` – implementation of mapping logic for [value objects](http://martinfowler.com/bliki/ValueObject.html) like `String`, `Integer` and so on.
* `Mapper.Read`, `Mapper.Write`, and `Mapper.ReadWrite` – implementation of mapping logic for updatable objects like JavaBeans.
* `Instance.Readable`, `Instance.Writable`, and `Instance.ReadWrite` – implementation of mapping logic for a specific object instance like UI form, list, or some other view.
* `Model` – a specialized implementation of mapping logic, which provides similar functionality to annotated JavaBeans in other ORM libraries (i.e. mixes mapping logic with storage of information), but without any annotations.
* `Reading.Many` – an implementation of reading logic that allows creation of different collections like `Set`, `Map`, and `SparseArray` from query results.

In conclusion, to answer the question why should you use this library, I will list all significant benefits of the library:
* It allows storing and retrieving of any type of object from a database.
* It provides an asynchronous mechanism to execute database operations based on the concept of promises and futures.
* It provides continuous queries that are refreshed whenever information in a database changes and can be used to automatically update information in UI.
* It provides functionality to define database migrations.
* It provides functionality to easily create a `ContentProvider`.
* It is built purely on top of Android SDK and the core part of the library does not require any additional libraries or drivers to work.
* It does not require any reflection logic or a precompile step to work.

I am sorry that there are no more details about the library, but they are being written. To quickly check out how does it look to use this ORM library and to give you a feeling about it, you can checkout the Tasks (a very simple App to store your ToDo list) in the [tasks](tasks) subdirectory. More specifically, checkout these classes:
* [Application](tasks/src/main/java/android/orm/tasks/Application.java) – creates a DAO object to be used by the whole application.
* [Activity](tasks/src/main/java/android/orm/tasks/Activity.java) – uses DAO to store and retrieve values from a database.
* [Task](tasks/src/main/java/android/orm/tasks/model/Task.java) – defines a Model class used by the application.
* [Migrations](tasks/src/main/java/android/orm/tasks/data/Migrations.java) – defines migrations for the database.
* [Provider](tasks/src/main/java/android/orm/tasks/data/Provider.java) – a ContentProvider in 10 lines of code.
