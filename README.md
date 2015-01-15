# Android ORM

This ORM is not yet another (annotiation-based) ORM library; it is my take on how an ORM should work. In my opinion, the problem with most other ORM libraries is that they always mix mapping logic (a logic that specifies how value is stored and retrieved from a database) with actual object that store information (e.g. annotated JavaBeans). Hence, they require developers to create specialized objects that can be stored and retrieved from the database and only this type of objects can be used in such manner.

On the other hand, the idea behind this ORM library is to separate objects that store information from the actual mapping logic. This allows us to use any object like String, JavaBeans, a 3rd-party library class, or even a Json object as a source of information that can be stored or retrieved. Of course, the mapping logic still needs to be specified by the developer before some object can be used to store and retrieve information from a database and this library provides many different ways for a developer to easily specify such mapping logics. These are:
* `Value`, `Value.Read`, `Value.Write`, and `Value.ReadWrite` – implementation of mapping logic for [value objects](http://martinfowler.com/bliki/ValueObject.html) like `String`, `Integer` and so on.
* `Mapper.Read`, `Mapper.Write`, and `Mapper.ReadWrite` – implementation of mapping logic for updatable objects like JavaBeans.
* `Instance.Readable`, `Instance.Writable`, and `Instance.ReadWrite` – implementation of mapping logic for a specific object instance like UI form, list, or some other view.
* `Model` – a specialized implementation of mapping logic, which provides similar functionality to annotated JavaBeans in other ORM libraries (i.e. mixes mapping logic with storage of information), but without any annotations.
* `Reading.Many` – an implementation of reading logic that allows creation of different collections like `Set`, `Map`, and `SparseArray` from query results.

To answer the question why should you use this library, I will list all significant benefits of the library:
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
