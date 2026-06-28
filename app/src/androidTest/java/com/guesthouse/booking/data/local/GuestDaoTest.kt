package com.guesthouse.booking.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.testutil.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuestDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var guestDao: GuestDao

    @Before
    fun setUp() {
        database = TestDatabase.createInMemoryDatabase()
        guestDao = database.guestDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById_roundTripsGuest() = runTest {
        val id = guestDao.insert(
            GuestEntity(name = "Alice Anderson", email = "alice@test.com", phone = "555-0101")
        )

        val stored = guestDao.getById(id)
        assertNotNull(stored)
        assertEquals("Alice Anderson", stored!!.name)
        assertEquals("alice@test.com", stored.email)
    }

    @Test
    fun update_persistsChanges() = runTest {
        val id = guestDao.insert(GuestEntity(name = "Bob Baker", email = "bob@test.com"))
        guestDao.update(GuestEntity(id = id, name = "Robert Baker", email = "robert@test.com"))

        val stored = guestDao.getById(id)
        assertEquals("Robert Baker", stored?.name)
        assertEquals("robert@test.com", stored?.email)
    }

    @Test
    fun setActive_excludesGuestFromActiveQuery() = runTest {
        val id = guestDao.insert(GuestEntity(name = "Carol Chen", email = "carol@test.com"))
        guestDao.setActive(id, false)

        val active = guestDao.observeActive().first()
        assertFalse(active.any { it.id == id })

        val all = guestDao.observeAllIncludingInactive().first()
        assertEquals(1, all.size)
        assertFalse(all.first().isActive)
    }
}
