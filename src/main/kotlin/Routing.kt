package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.*
import java.util.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone

fun Application.configureRouting() {
    routing {
        route("/auth") {
            post("/register") {
                val request = call.receive<RegisterRequest>()
                try {
                    transaction {
                        Users.insert {
                            it[uid] = request.uid
                            it[email] = request.email
                            it[first_name] = request.first_name
                            it[last_name] = request.last_name
                            it[phone] = request.phone
                        }
                    }


                    println("User registered successfully: ${request.email}")

                    // successful user creation
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(Code = 200, Message = "User Created Sucessfully")
                    )
                } //unsuccessful requests
                catch (e: ExposedSQLException) {
                    if (e.message?.contains("duplicate key value violates unique constraint") == true) {
                        println("Registration failed: Duplicate UID or Email for ${request.email}")
                        call.respond(
                            HttpStatusCode.Conflict,
                            ApiResponse(Code = 409, Message = "User with this UID or Email already exists")
                        )
                    } else {
                        println("Database error during registration: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse(Code = 500, Message = "Internal Server Error during registration")
                        )
                    }
                } catch (e: Exception) {
                    println("An unexpected error occurred during registration: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse(Code = 500, Message = "An unexpected error occurred")
                    )
                }
            }

        }

        route("/user") {
            get("profile/{uid}") {
                val uid = call.parameters["uid"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "UID is missing")
                )
                try {
                    val userProfile = transaction {
                        Users.select { Users.uid eq uid }
                            .map { row ->

                               //Debugging purpose
//                                println("DEBUG: Full Row content for UID $uid: $row")
//                                println("DEBUG: uid from row: ${row[Users.uid]}")
//                                println("DEBUG: email from row: ${row[Users.email]}")
//                                println("DEBUG: first_name from row: ${row[Users.first_name]}")
//                                println("DEBUG: last_name from row: ${row[Users.last_name]}")
//                                println("DEBUG: phone from row: ${row[Users.phone]}")

                                UserProfileResponse(
                                    email = row[Users.email],
                                    first_name = row[Users.first_name],
                                    last_name = row[Users.last_name],
                                    phone = row[Users.phone]
                                )
                            }
                            .singleOrNull()
                    }

                    if (userProfile != null) {
                        call.respond(HttpStatusCode.OK, userProfile)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "User profile not found for provided UID")
                        )
                    }
                } catch (e: Exception) {
                    println("Error fetching user profile for UID: $uid: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Message" to "An unexpected error occurred"))
                }
            }

            get("/bookings/{uid}"){
                val uid = call.parameters["uid"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "UID is missing")
                )

                try{
                    val userBookings = transaction {
                        Bookings.select{
                            Bookings.uid eq uid
                        }
                            .map { row ->
                                UserBookingItem(
                                    bookingId = row[Bookings.bookingid].toString(),
                                    movieTitle = row[Bookings.movietitle],
                                    theaterName = row[Bookings.theaterName],
                                    showDate = LocalDate.parse(row[Bookings.showdate]),
                                    showTimings = row[Bookings.showtime],
                                    seats = row[Bookings.seatsbooked],
                                    totalAmount = row[Bookings.totalamount],
                                    bookingTime = row[Bookings.bookingtime]
                                )
                            }
                    }
                    if (userBookings.isNotEmpty()) {
                        call.respond(
                            HttpStatusCode.OK,
                            UserBookingsResponse(code = HttpStatusCode.OK.value, bookings = userBookings)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse(Code = HttpStatusCode.NotFound.value, Message = "No bookings found for user ID: $uid")
                        )
                    }

                } catch (e: Exception) {
                    call.application.log.error("Error fetching bookings for user ID: $uid", e)
                    call.application.log.error("Error fetching bookings for user ID: $uid", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(HttpStatusCode.InternalServerError.value, "An unexpected error occurred while fetching bookings."))
                }
                }

            put("profile/{uid}") {
                val uid = call.parameters["uid"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("message" to "UID is missing"))
                val request = call.receive<UserProfileUpdateRequest>()

                try{
                    val updatedRowCount = transaction {
                        Users.update({ Users.uid eq uid }) {
                            request.email?.let { email -> it[Users.email] = email }
                            request.first_name?.let { first_name -> it[Users.first_name] = first_name }
                            request.last_name?.let { last_name -> it[Users.last_name] = last_name }
                            request.phone?.let { phone -> it[Users.phone] = phone }
                        }
                    }
                    if (updatedRowCount > 0) {
                        println("User profile updated successfully for UID: $uid")
                        call.respond(HttpStatusCode.OK, ApiResponse(HttpStatusCode.OK.value, "User profile updated successfully"))
                    } else {
                        println("User profile not found for update for UID: $uid")
                        call.respond(HttpStatusCode.NotFound,ApiResponse(HttpStatusCode.NotFound.value,"User profile not found for update" ))
                    }
                } catch (e: Exception) {
                    println("Error updating user profile for UID $uid: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "An unexpected error occurred during profile update")
                    )
                }
            }
        }

        route("/movies"){
            get("/{movieid}/trailer") {
                val movieIdParam = call.parameters["movieId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse(Code = HttpStatusCode.BadRequest.value, Message = "Missing movieId"))
                    return@get
                }

                try {


                    val trailer = transaction{
                    Trailers.select { Trailers.movieid eq movieIdParam }
                        .map { row ->
                            Trailer(
                                url = row[Trailers.url]
                            )
                        }
                        .firstOrNull()
                }
                    if(trailer!=null){
                        call.respond(HttpStatusCode.OK,
                            ApiResponse(Code = HttpStatusCode.OK.value, "${trailer.url}"))
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            ApiResponse(Code = HttpStatusCode.NotFound.value, "Trailer not found"))
                    }
                } catch (e: IllegalArgumentException) {
                    // Catch error if the provided movieId is not a valid UUID format
                    call.respond(HttpStatusCode.BadRequest, ApiResponse(Code = HttpStatusCode.BadRequest.value, Message = "Invalid movie ID format: $movieIdParam"))
                } catch (e: Exception) {
                    // Catch any other unexpected errors during database operation
                    call.application.log.error("Error fetching trailer for movie ID: $movieIdParam", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(Code = HttpStatusCode.InternalServerError.value, Message = "An unexpected error occurred."))
                }
            }

            get("/{movieId}/theaters"){
                val movieIdParam = call.parameters["movieId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse(Code = HttpStatusCode.BadRequest.value, Message = "Missing movieId"))
                    return@get
                }
                try {
                    val theatersForMovie = transaction {
                        MovieShowings.select { MovieShowings.movieid eq movieIdParam }
                            .map { row ->
                                TheaterMovieShowingDetails(
                                    theaterId = row[MovieShowings.theaterid],
                                    name = row[MovieShowings.theatername],
                                    location = row[MovieShowings.theaterlocation],
                                    showTimings = row[MovieShowings.showtimings].split(",").map { it.trim() }, // Parse comma-separated string to list
                                    movieExpiryDateInTheater = row[MovieShowings.movieexpirydate],
                                    seatPrices = row[MovieShowings.seatprices]
                                )
                            }
                            .distinctBy { it.theaterId } // Ensure unique theaters if multiple showings exist for same movie/theater
                    }

                    if (theatersForMovie.isNotEmpty()) {
                        call.respond(
                            HttpStatusCode.OK,
                            TheatersForMovieResponse(code = HttpStatusCode.OK.value, theaters = theatersForMovie)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse(Code = HttpStatusCode.NotFound.value, Message = "No theaters found for movie ID: $movieIdParam")
                        )
                    }

                } catch (e: Exception) {
                    call.application.log.error("Error fetching theaters for movie ID: $movieIdParam", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(Code = HttpStatusCode.InternalServerError.value, Message = "An unexpected error occurred."))
                }
            }

            // GET /movies?location={userlocation} endpoint
            get {
                val userLocation = call.request.queryParameters["location"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse(Code = HttpStatusCode.BadRequest.value, Message = "Location query parameter is required"))
                    return@get
                }

                try {
                    val moviesByLocation = transaction {
                        Movies.select { Movies.location eq userLocation }
                            .map { row ->
                                MovieItem(
                                    movieId = row[Movies.movieid],
                                    movieName = row[Movies.title],
                                    posterUrl = row[Movies.poster_url],
                                    genre = row[Movies.genre]
                                )
                            }
                    }

                    if (moviesByLocation.isNotEmpty()) {
                        call.respond(
                            HttpStatusCode.OK,
                            MoviesByLocationResponse(code = HttpStatusCode.OK.value, movies = moviesByLocation)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse(Code = HttpStatusCode.NotFound.value, Message = "No movies found for location: $userLocation")
                        )
                    }

                } catch (e: Exception) {
                    call.application.log.error("Error fetching movies for location: $userLocation", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(Code = HttpStatusCode.InternalServerError.value, Message = "An unexpected error occurred."))
                }
            }
            get("/{movieId}") {
                val movieIdParam = call.parameters["movieId"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(Code = HttpStatusCode.BadRequest.value, Message = "Missing movieId")
                    )
                    return@get
                }

                try {
                    val movieDetail = transaction {
                        MovieDetails.select { MovieDetails.movieid eq movieIdParam }
                            .map { row ->
                                MovieDetail(
                                    movieId = row[MovieDetails.movieid],
                                    title = row[MovieDetails.title],
                                    rating = row[MovieDetails.rating],
                                    language = row[MovieDetails.language],
                                    description = row[MovieDetails.description],
                                    runtime = row[MovieDetails.duration], // Maps to 'duration' in DB
                                    coverpicture = row[MovieDetails.coverpicture_url], // Maps to 'coverpicture_url' in DB
                                    genre = row[MovieDetails.genre],
                                    format = row[MovieDetails.format]
                                )
                            }
                            .singleOrNull()
                    }

                    if (movieDetail != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            MovieDetailResponse(code = HttpStatusCode.OK.value, movie = movieDetail)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse(
                                Code = HttpStatusCode.NotFound.value,
                                Message = "Movie details not found for ID: $movieIdParam"
                            )
                        )
                    }

                } catch (e: Exception) {
                    call.application.log.error("Error fetching movie details for ID: $movieIdParam", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse(
                            Code = HttpStatusCode.InternalServerError.value,
                            Message = "An unexpected error occurred."
                        )
                    )
                }
            }

        }

        route("/bookings") {
            post("/confirm") {
                val request = call.receive<BookingRequest>()

                try {
                    val requestedSeatsString = request.seats.joinToString(",")
                    val showId = request.showid
                    val showDate = request.showdate
                    val existingBooking = transaction {
                        Bookings.select {
                            (Bookings.showid eq showId) and
                                    (Bookings.showdate eq showDate) and
                                    (Bookings.seatsbooked eq requestedSeatsString)
                        }.singleOrNull()
                    }
                    if (existingBooking != null) {

                        call.respond(
                            HttpStatusCode.NotFound, // Returning 404 Not Found as specifically requested
                            ApiResponse(
                                Code = HttpStatusCode.NotFound.value,
                                Message = "A booking with the exact same show, date, and seats already exists."
                            )
                        )
                        return@post // Exit the function
                    }
                    val newBookingId = transaction {
                        val insertedBookingId = Bookings.insert {
                            it[uid] = request.uid
                            it[movietitle] = request.movietitle
                            it[showdate] = request.showdate
                            it[showtime] = request.showtime
                            it[showid] = request.showid
                            it[theaterName] = request.theaterName
                            it[seatsbooked] = request.seats.joinToString(",") // Store as comma-separated string
                            it[totalamount] = request.totalAmount
                            it[bookingtime] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString() // Store as ISO String
                            it[paymentid] = request.paymentId
                        } get Bookings.bookingid

                        insertedBookingId
                    }

                    call.respond(
                        HttpStatusCode.Created,
                        BookingConfirmationResponse(
                            code = HttpStatusCode.Created.value,
                            message = "Booking confirmed successfully",
                            bookingId = newBookingId.toString(),
                            totalAmount = request.totalAmount
                        )
                    )

                } catch (e: Exception) {
                    call.application.log.error("Error confirming booking: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(HttpStatusCode.InternalServerError.value, "An unexpected error occurred during booking confirmation."))
                }
            }
        }
        route("/showings") {
            get("/{showId}/{showdate}/booked-seats") {
                val showId = call.parameters["showId"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(HttpStatusCode.BadRequest.value, "Show ID path parameter is missing.")
                    )
                    return@get
                }
                val showDate = call.parameters["showdate"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse(HttpStatusCode.BadRequest.value, "Show Date path parameter is missing.")
                    )
                    return@get
                }

                try {
                    val allBookedSeats = transaction {
                        Bookings.select {
                            (Bookings.showid eq showId) and (Bookings.showdate eq showDate)
                        }
                            .flatMap { row ->
                                row[Bookings.seatsbooked].split(",").map { it.trim() }
                            }
                    }

                    if (allBookedSeats.isNotEmpty()) {
                        call.respond(
                            HttpStatusCode.OK,
                            BookedSeatsResponse(
                                code = HttpStatusCode.OK.value,
                                showId = showId,
                                bookedSeats = allBookedSeats.distinct()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            BookedSeatsResponse(
                                code = HttpStatusCode.OK.value,
                                showId = showId,
                                bookedSeats = emptyList()
                            )
                        )
                    }

                } catch (e: Exception) {
                    call.application.log.error("Error fetching booked seats for show ID: $showId", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse(
                            HttpStatusCode.InternalServerError.value,
                            "An unexpected error occurred while fetching booked seats."
                        )
                    )
                }
            }
        }
    }
}
