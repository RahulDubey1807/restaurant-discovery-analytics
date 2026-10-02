# Restaurant Discovery & Analytics System

A menu-driven Scala CLI application for the MongoDB Atlas assignment. It connects to the official `sample_restaurants.restaurants` collection and implements CRUD, five search filters, two indexes (including a compound index), and three MongoDB aggregation reports.

## Requirements

- JDK 17
- Scala CLI
- MongoDB Atlas access to a cluster where the `sample_restaurants` sample dataset has been loaded

The project pins Scala 2.13.16 and MongoDB's official Scala driver 5.11.1 in `src/main/scala/Main.scala`. Scala CLI downloads the declared driver dependency on the first run.

## 1. Load the Atlas sample dataset

In Atlas, open the cluster menu and choose **Load Sample Dataset**. In Data Explorer, verify that `sample_restaurants` contains the `restaurants` collection.

## 2. Protect your connection string

Because a database password was included in the original chat, rotate that Atlas database user's password before running this project. Do not put the URI in the source code, README, screenshots, or a public repository. In Atlas, also make sure your current IP address is allowed under Network Access.

In PowerShell, set the URI for the current terminal session. The prompt input is not saved in this project's files:

```powershell
$env:MONGODB_URI = Read-Host "Paste your rotated MongoDB Atlas connection URI"
```

The URI should point at your Atlas cluster and include valid credentials. The application itself selects database `sample_restaurants` and collection `restaurants`.

## 3. Run the application

From the unzipped `restaurant-discovery` folder:

```powershell
scala-cli run src/main/scala/Main.scala --jvm 17
```

The first run may take a little longer while Scala CLI downloads the MongoDB driver. If the program says `MONGODB_URI is not set`, set the environment variable in the same PowerShell window and run the command again.

## Menu and assignment coverage

1. **Add restaurant** - creates a document with a unique `student-...` `restaurant_id`. Add at least two for the assignment.
2. **Search / view** - name, cuisine, borough, ZIP code, minimum grade score, or recent documents.
3. **Update** - changes the name of a record using its `restaurant_id`.
4. **Delete** - deletes a record using its `restaurant_id`; the app only permits IDs beginning with `student-` as a safety measure.
5. **Analytics** - restaurant counts by cuisine, restaurant counts by borough, and average grade score by cuisine.
6. **Create / show indexes** - creates a name index and a compound borough+cuisine index, then lists indexes.
7. **Exit**

The code uses a case class for restaurant input, encapsulates MongoDB operations in classes, uses methods and pattern matching, processes input with Scala collection `map` and `filter`, and handles errors with `try`/`catch`. See `Main.scala` for the implementation.

## Screenshot checklist

Capture screenshots of:

- Adding two restaurants and showing their generated IDs
- Searching by at least three criteria
- Updating one added record and deleting another added record
- Listing the indexes
- Each of the three analytics reports

Save your own screenshots in the `screenshots` folder. Before submitting or publishing, inspect the screenshots and source for credentials.

## Notes

- The app's score filter matches a restaurant if **any** element in `grades` has a score at or above the threshold.
- The average score report unwinds each restaurant's `grades` array, so each grade entry contributes to that cuisine's average.
- The two assignment-created indexes are `name_1` and the compound `borough_1_cuisine_1`. The collection also has MongoDB's default `_id_` index.
- CRUD changes affect the live Atlas sample collection. The delete option is restricted to app-created IDs, and you should test with your own records.
