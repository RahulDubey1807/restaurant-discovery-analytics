//> using scala 2.13.16
//> using jvm 17
//> using dep org.mongodb.scala::mongo-scala-driver:5.11.1

import org.mongodb.scala._
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

import java.util.Date
import java.util.UUID

object Main {

  // ------------------------------------------------------------
  // DATABASE SETTINGS
  // ------------------------------------------------------------

  private val mongoUri =
    sys.env.getOrElse("MONGODB_URI", "mongodb://localhost:27017")

  private val databaseName = "sample_restaurants"
  private val collectionName = "restaurants"

  private val timeout = 30.seconds


  // ------------------------------------------------------------
  // CASE CLASS
  // ------------------------------------------------------------

  case class Restaurant(
      name: String,
      cuisine: String,
      borough: String,
      zipcode: String
  ) {

    // Convert Scala object into MongoDB document
    def toDocument(id: String): Document = {

      Document(
        "address" -> Document(
          "building" -> "",
          "coord" -> List.empty[Double],
          "street" -> "",
          "zipcode" -> zipcode
        ),
        "borough" -> borough,
        "cuisine" -> cuisine,
        "grades" -> List(
          Document(
            "date" -> new Date(),
            "grade" -> "A",
            "score" -> 10
          )
        ),
        "name" -> name,
        "restaurant_id" -> id
      )
    }
  }


  // ------------------------------------------------------------
  // MAIN METHOD
  // ------------------------------------------------------------

  def main(args: Array[String]): Unit = {

    val client = MongoClient(mongoUri)

    try {

      val database = client.getDatabase(databaseName)
      val collection = database.getCollection(collectionName)

      println()
      println("Connected to sample_restaurants.restaurants")
      println()

      val app = new RestaurantApp(collection)

      // Create indexes when application starts
      app.createIndexes()

      app.run()

    } catch {

      case NonFatal(error) =>
        println()
        println("Error: " + error.getMessage)
        println()
        println("Make sure MongoDB is running.")
        println("Expected local connection: mongodb://localhost:27017")

    } finally {

      client.close()
    }
  }


  // ------------------------------------------------------------
  // RESTAURANT APPLICATION
  // ------------------------------------------------------------

  class RestaurantApp(collection: MongoCollection[Document]) {

    private val repository = new RestaurantRepository(collection)
    private val analytics = new RestaurantAnalytics(collection)


    // ----------------------------------------------------------
    // MAIN MENU
    // ----------------------------------------------------------

    def run(): Unit = {

      var running = true

      while (running) {

        println()
        println("==========================================")
        println(" Restaurant Discovery & Analytics System")
        println("==========================================")
        println("1. Add restaurant")
        println("2. Search / View restaurants")
        println("3. Update restaurant")
        println("4. Delete restaurant")
        println("5. Restaurant analytics")
        println("6. Create / Show indexes")
        println("7. Exit")
        println("==========================================")

        val choice = read("Enter choice: ")

        choice match {

          case "1" =>
            safe(repository.addRestaurant())

          case "2" =>
            safe(searchMenu())

          case "3" =>
            safe(repository.updateRestaurant())

          case "4" =>
            safe(repository.deleteRestaurant())

          case "5" =>
            safe(analyticsMenu())

          case "6" =>
            safe(repository.showIndexes())

          case "7" =>
            running = false

          case _ =>
            println("Invalid choice. Enter a number from 1 to 7.")
        }
      }

      println()
      println("Goodbye!")
    }


    // ----------------------------------------------------------
    // SEARCH MENU
    // ----------------------------------------------------------

    private def searchMenu(): Unit = {

      println()
      println("--------------- Search Menu ---------------")
      println("1. Search by restaurant name")
      println("2. Search by cuisine")
      println("3. Search by borough")
      println("4. Search by ZIP code")
      println("5. Search by minimum score")
      println("6. Search by cuisine + borough")
      println("7. View recent restaurants")
      println("-------------------------------------------")

      val choice = read("Enter choice: ")

      choice match {

        case "1" =>
          val name = read("Enter restaurant name: ")
          repository.searchByName(name)

        case "2" =>
          val cuisine = read("Enter cuisine: ")
          repository.searchByCuisine(cuisine)

        case "3" =>
          val borough = read("Enter borough: ")
          repository.searchByBorough(borough)

        case "4" =>
          val zipcode = read("Enter ZIP code: ")
          repository.searchByZip(zipcode)

        case "5" =>
          val score = read("Enter minimum score: ").toInt
          repository.searchByScore(score)

        case "6" =>
          val cuisine = read("Enter cuisine: ")
          val borough = read("Enter borough: ")
          repository.searchByCuisineAndBorough(cuisine, borough)

        case "7" =>
          repository.viewRecent()

        case _ =>
          println("Invalid search choice.")
      }
    }


    // ----------------------------------------------------------
    // ANALYTICS MENU
    // ----------------------------------------------------------

    private def analyticsMenu(): Unit = {

      println()
      println("------------- Analytics Menu --------------")
      println("1. Restaurants by cuisine")
      println("2. Restaurants by borough")
      println("3. Average score by cuisine")
      println("4. Scala collection groupBy demo")
      println("-------------------------------------------")

      val choice = read("Enter choice: ")

      choice match {

        case "1" =>
          analytics.restaurantsByCuisine()

        case "2" =>
          analytics.restaurantsByBorough()

        case "3" =>
          analytics.averageScoreByCuisine()

        case "4" =>
          analytics.scalaGroupByDemo()

        case _ =>
          println("Invalid analytics choice.")
      }
    }


    // ----------------------------------------------------------
    // ERROR HANDLING
    // ----------------------------------------------------------

    private def safe(action: => Unit): Unit = {

      try {
        action
      } catch {

        case NonFatal(error) =>
          println()
          println("Operation failed: " + error.getMessage)
      }
    }


    // ----------------------------------------------------------
    // INDEX CREATION
    // ----------------------------------------------------------

    def createIndexes(): Unit = {

      repository.createIndexes()
    }
  }


  // ============================================================
  // RESTAURANT REPOSITORY
  // ============================================================

  class RestaurantRepository(collection: MongoCollection[Document]) {


    // ----------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------

    def addRestaurant(): Unit = {

      println()
      println("----------- Add Restaurant ---------------")

      val name = read("Restaurant name: ").trim
      val cuisine = read("Cuisine: ").trim
      val borough = read("Borough: ").trim
      val zipcode = read("ZIP code: ").trim

      if (name.isEmpty || cuisine.isEmpty || borough.isEmpty || zipcode.isEmpty) {

        throw new IllegalArgumentException(
          "All fields are required."
        )
      }

      // Create a unique ID for the restaurant
      val id = "student-" + UUID.randomUUID().toString

      val restaurant = Restaurant(
        name,
        cuisine,
        borough,
        zipcode
      )

      awaitUnit(
        collection.insertOne(
          restaurant.toDocument(id)
        )
      )

      println()
      println("Restaurant added successfully!")
      println("Restaurant ID: " + id)
      println("Save this ID for update/delete.")
    }


    // ----------------------------------------------------------
    // READ / SEARCH BY NAME
    // ----------------------------------------------------------

    def searchByName(name: String): Unit = {

      val filter =
        Filters.regex(
          "name",
          java.util.regex.Pattern.quote(name),
          "i"
        )

      val results =
        await(
          collection
            .find(filter)
            .limit(10)
        )

      showResults(results)
    }


    // ----------------------------------------------------------
    // SEARCH BY CUISINE
    // ----------------------------------------------------------

    def searchByCuisine(cuisine: String): Unit = {

      val filter =
        Filters.equal("cuisine", cuisine)

      val results =
        await(
          collection
            .find(filter)
            .limit(10)
        )

      showResults(results)
    }


    // ----------------------------------------------------------
    // SEARCH BY BOROUGH
    // ----------------------------------------------------------

    def searchByBorough(borough: String): Unit = {

      val filter =
        Filters.equal("borough", borough)

      val results =
        await(
          collection
            .find(filter)
            .limit(10)
        )

      showResults(results)
    }


    // ----------------------------------------------------------
    // SEARCH BY ZIP CODE
    // ----------------------------------------------------------

    def searchByZip(zipcode: String): Unit = {

      val filter =
        Filters.equal("address.zipcode", zipcode)

      val results =
        await(
          collection
            .find(filter)
            .limit(10)
        )

      showResults(results)
    }


    // ----------------------------------------------------------
    // SEARCH BY SCORE
    // ----------------------------------------------------------

    def searchByScore(score: Int): Unit = {

      val filter =
        Filters.gte("grades.score", score)

      val results =
        await(
          collection
            .find(filter)
            .limit(10)
        )

      showResults(results)
    }


    // ----------------------------------------------------------
    // SEARCH BY CUISINE + BOROUGH
    // ----------------------------------------------------------

    def searchByCuisineAndBorough(
        cuisine: String,
        borough: String
    ): Unit = {

      val filter =
        Filters.and(
          Filters.equal("cuisine", cuisine),
          Filters.equal("borough", borough)
        )

      val results =
        await(
          collection
            .find(filter)
            .limit(10)
        )

      showResults(results)
    }


    // ----------------------------------------------------------
    // VIEW RECENT RESTAURANTS
    // ----------------------------------------------------------

    def viewRecent(): Unit = {

      val results =
        await(
          collection
            .find()
            .limit(10)
        )

      showResults(results)
    }


    // ----------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------

    def updateRestaurant(): Unit = {

      println()
      println("----------- Update Restaurant ------------")

      val id =
        read("Enter restaurant_id to update: ")

      val newName =
        read("Enter new restaurant name: ")

      if (id.isEmpty || newName.isEmpty) {

        throw new IllegalArgumentException(
          "ID and new name are required."
        )
      }

      val result =
        awaitOne(
          collection.updateOne(
            Filters.equal("restaurant_id", id),
            Updates.set("name", newName)
          )
        )

      println()
      println("Matched: " + result.getMatchedCount)
      println("Modified: " + result.getModifiedCount)

      if (result.getMatchedCount == 0) {

        println("No restaurant found with that ID.")

      } else {

        println("Restaurant updated successfully!")
      }
    }


    // ----------------------------------------------------------
    // DELETE
    // ----------------------------------------------------------

    def deleteRestaurant(): Unit = {

      println()
      println("----------- Delete Restaurant ------------")

      val id =
        read("Enter restaurant_id to delete: ")

      // Safety: only delete records created by this application
      if (!id.startsWith("student-")) {

        println()
        println("Safety check:")
        println("This application only deletes restaurants")
        println("whose ID starts with 'student-'.")
        return
      }

      val result =
        awaitOne(
          collection.deleteOne(
            Filters.equal("restaurant_id", id)
          )
        )

      println()
      println("Deleted: " + result.getDeletedCount)

      if (result.getDeletedCount == 1) {

        println("Restaurant deleted successfully!")

      } else {

        println("No restaurant found with that ID.")
      }
    }


    // ----------------------------------------------------------
    // CREATE INDEXES
    // ----------------------------------------------------------

    def createIndexes(): Unit = {

      // Index 1: restaurant name
      await(
        collection.createIndex(
          Indexes.ascending("name")
        )
      )

      // Index 2: compound index
      // Used by cuisine + borough search
      await(
        collection.createIndex(
          Indexes.compoundIndex(
            Indexes.ascending("borough"),
            Indexes.ascending("cuisine")
          )
        )
      )

      println()
      println("Indexes created/confirmed:")
      println("1. name_1")
      println("2. borough_1_cuisine_1")
    }


    // ----------------------------------------------------------
    // SHOW INDEXES
    // ----------------------------------------------------------

    def showIndexes(): Unit = {

      println()
      println("-------------- Indexes -------------------")

      val indexes =
        await(
          collection.listIndexes()
        )

      indexes.foreach { index =>

        println(index.toJson())
      }
    }


    // ----------------------------------------------------------
    // PRINT SEARCH RESULTS
    // ----------------------------------------------------------

    private def showResults(
        documents: Seq[Document]
    ): Unit = {

      println()

      if (documents.isEmpty) {

        println("No restaurants found.")

      } else {

        println("Restaurants found: " + documents.size)
        println()

        documents.foreach { document =>
          println(document.toJson())
          println("-------------------------------------------")
        }
      }
    }
  }


  // ============================================================
  // ANALYTICS
  // ============================================================

  class RestaurantAnalytics(collection: MongoCollection[Document]) {


    // ----------------------------------------------------------
    // 1. RESTAURANTS BY CUISINE
    // ----------------------------------------------------------

    def restaurantsByCuisine(): Unit = {

      val pipeline = Seq(

        Aggregates.group(
          "$cuisine",
          Accumulators.sum(
            "restaurantCount",
            1
          )
        ),

        Aggregates.sort(
          Sorts.descending("restaurantCount")
        ),

        Aggregates.limit(10)
      )

      val results =
        await(
          collection
            .aggregate[Document](pipeline)
        )

      printResults(
        "Restaurants by Cuisine",
        results
      )
    }


    // ----------------------------------------------------------
    // 2. RESTAURANTS BY BOROUGH
    // ----------------------------------------------------------

    def restaurantsByBorough(): Unit = {

      val pipeline = Seq(

        Aggregates.group(
          "$borough",
          Accumulators.sum(
            "restaurantCount",
            1
          )
        ),

        Aggregates.sort(
          Sorts.descending("restaurantCount")
        ),

        Aggregates.limit(10)
      )

      val results =
        await(
          collection
            .aggregate[Document](pipeline)
        )

      printResults(
        "Restaurants by Borough",
        results
      )
    }


    // ----------------------------------------------------------
    // 3. AVERAGE SCORE BY CUISINE
    // ----------------------------------------------------------

    def averageScoreByCuisine(): Unit = {

      val pipeline = Seq(

        // Convert each grade into a separate document
        Aggregates.unwind("$grades"),

        // Only keep grades that contain a score
        Aggregates.`match`(
          Filters.exists("grades.score", true)
        ),

        // Group by cuisine and calculate average score
        Aggregates.group(
          "$cuisine",
          Accumulators.avg(
            "averageScore",
            "$grades.score"
          )
        ),

        // Lower score first
        Aggregates.sort(
          Sorts.ascending("averageScore")
        ),

        Aggregates.limit(10)
      )

      val results =
        await(
          collection
            .aggregate[Document](pipeline)
        )

      printResults(
        "Average Score by Cuisine",
        results
      )
    }


    // ----------------------------------------------------------
    // SCALA COLLECTION DEMONSTRATION
    // ----------------------------------------------------------

    def scalaGroupByDemo(): Unit = {

      val documents =
        await(
          collection
            .find()
            .limit(20)
        )

      // map
      val cuisines =
        documents.map { document =>
          document.getString("cuisine")
        }

      // filter
      val validCuisines =
        cuisines.filter(_.nonEmpty)

      // groupBy
      val grouped =
        validCuisines.groupBy(identity)

      println()
      println("Scala Collections Demo")
      println("-------------------------------------------")
      println("map -> gets cuisine from each document")
      println("filter -> removes empty cuisines")
      println("groupBy -> groups restaurants by cuisine")
      println()

      grouped.foreach {
        case (cuisine, restaurants) =>
          println(cuisine + " -> " + restaurants.size)
      }
    }


    // ----------------------------------------------------------
    // PRINT AGGREGATION RESULTS
    // ----------------------------------------------------------

    private def printResults(
        title: String,
        results: Seq[Document]
    ): Unit = {

      println()
      println(title)
      println("-------------------------------------------")

      if (results.isEmpty) {

        println("No results found.")

      } else {

        results.foreach { result =>
          println(result.toJson())
        }
      }
    }
  }


  // ============================================================
  // HELPER FUNCTIONS
  // ============================================================

  private def read(prompt: String): String = {

    print(prompt)

    val input =
      scala.io.StdIn.readLine()

    if (input == null) ""
    else input.trim
  }


private def await[T](
    observable: Observable[T]
): Seq[T] = {

  Await.result(
    observable.toFuture(),
    timeout
  )
}


  private def awaitOne[T](
      observable: Observable[T]
  ): T = {

    Await.result(
      observable.toFuture(),
      timeout
    ).head
  }


  private def awaitUnit[T](
      observable: Observable[T]
  ): Unit = {

    Await.result(
      observable.toFuture(),
      timeout
    )

    ()
  }
}