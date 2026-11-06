package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PharmacyRepository
import com.example.model.ResourceCategory
import com.example.model.SubjectResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches S Perfect Pharmacy`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("S Perfect Pharmacy", appName)
    }

    @Test
    fun `repository provides pharmacy study materials`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PharmacyRepository(context)
        val list = repository.getAllResources().first()
        assertNotNull(list)
    }

    @Test
    fun `owner pin default verification works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PharmacyRepository(context)
        assertTrue(repository.verifyOwnerPin("1234"))
    }

    @Test
    fun `change owner pin updates successfully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PharmacyRepository(context)
        val changed = repository.changeOwnerPin("1234", "9876")
        assertTrue(changed)
        assertTrue(repository.verifyOwnerPin("9876"))
    }
}
