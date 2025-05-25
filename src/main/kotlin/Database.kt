package com.example

import com.example.Users.uid
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.Column


//users table
object Users : Table("users") {
    val uid = varchar("uid", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val first_name = varchar("first_name", 100)
    val last_name = varchar("last_name", 100)
    val phone = varchar("phone", 20)

    override val primaryKey = PrimaryKey(uid)
}

//trailers table
object Trailers : Table("trailers"){
    val movieid = varchar("movie_id", 255).uniqueIndex()
    val url = varchar("url",500)
}


//Movieshowings table
object MovieShowings : Table("movie_showings"){
    val id = varchar("id",255).uniqueIndex()
    val movieid =varchar("movie_id",255)
    val theaterid = varchar("theater_id",255)
    val theatername = varchar("theater_name",255)
    val theaterlocation = varchar("theater_location",255)
    val showtimings = varchar("show_timings",255)
    val movieexpirydate = varchar("movie_expiry_date",255)
    val seatprices = text("seat_prices")

    override val primaryKey = PrimaryKey(id)
}

//movies table for home page
object Movies : Table("movies"){
    val movieid = varchar("movie_id", 255)
    val title = varchar("title", 100)
    val poster_url = varchar("poster_url", 500)
    val genre = varchar("genre", 100)
    val location = varchar("location", 100)

}

//movie details table for movie page
object MovieDetails : Table("movie_details"){
    val movieid = varchar("movie_id", 255).uniqueIndex()
    val title = varchar("title", 100)
    val rating = double("rating")
    val language = varchar("language", 50)
    val description = text("description")
    val duration = varchar("duration", 10)
    val coverpicture_url = varchar("coverpicture_url", 500)
    val genre = varchar("genre", 100)
    val format = varchar("format", 50)

    override val primaryKey = PrimaryKey(movieid)
}

//Bookings table
object Bookings : Table("bookings"){
    val showid = varchar("show_id", 255)
    val uid = varchar("uid", 255)
    val bookingid = uuid("booking_id").uniqueIndex().autoGenerate()
    val movietitle = varchar("movie_title", 255)
    val showtime = varchar("show_time", 255)
    val showdate = varchar("show_date", 255)
    val seatsbooked = text("seats_booked") // e.g., "A1,A2,B3"
    val totalamount = double("total_amount")
    val bookingtime = varchar("booking_time", 255)
    val theaterName = varchar("theater_name", 255)
    val paymentid = varchar("payment_id", 255).nullable()

    override val primaryKey = PrimaryKey(bookingid)

}
fun Application.initDatabase() {
    // Read database configuration from application.conf
    val config = environment.config.config("ktor.database")
    val url = config.property("jdbcUrl").getString()
    val driver = config.property("driver").getString()
    val user = config.property("user").getString()
    val password = config.property("password").getString()

    Database.connect(url, driver, user, password)
    transaction {
        SchemaUtils.create(Users) // This line creates the table if it doesn't exist.
        // If the table exists but has an old schema, it will NOT update it.
        SchemaUtils.create(Trailers)

        SchemaUtils.create(MovieShowings)

        SchemaUtils.create(Movies)

        SchemaUtils.create(MovieDetails)

        SchemaUtils.create(Bookings)
    }

    println("Database connected and 'users' table schema confirmed (not created).")
}
