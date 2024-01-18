package com.popovycha.criminalIntent

import android.content.Context
import androidx.room.Room
import com.popovycha.criminalIntent.database.CrimeDatabase
import com.popovycha.criminalIntent.database.migration_1_2
import com.popovycha.criminalIntent.database.migration_2_3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val DATABASE_NAME = "crime-database"
class CrimeRepository private constructor(context: Context, private val coroutineScope: CoroutineScope = GlobalScope) {

    //to store a reference to your database
    private val database: CrimeDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            CrimeDatabase::class.java,
            DATABASE_NAME
        )
        //.fallbackToDestructiveMigration()
        .addMigrations(migration_2_3)
        .build()

    //call through to those implementations from your repository
    suspend fun getCrimes(): Flow<List<Crime>> {
        return withContext(Dispatchers.IO) {
            database.crimeDao().getCrimes()
        }
    }

    suspend fun getCrime(id: UUID) : Crime {
        return withContext(Dispatchers.IO) {
            database.crimeDao().getCrime(id)
        }
    }

     fun updateCrime(crime: Crime) {
         coroutineScope.launch {
            database.crimeDao().updateCrime(crime)
        }
    }

    fun addCrime(crime: Crime) {
        coroutineScope.launch {
            database.crimeDao().addCrime(crime)
        }
    }
    companion object {
        private var INSTANCE: CrimeRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }
        fun get(): CrimeRepository {
            return INSTANCE ?:
            throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}
