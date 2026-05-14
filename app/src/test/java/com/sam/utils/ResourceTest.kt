package com.sam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceTest {

    @Test
    fun resourceSuccess_containsDataAndNoMessage() {
        val data = "Test Data"
        val resource = Resource.Success(data)

        assertTrue(resource is Resource.Success)
        assertEquals(data, resource.data)
        assertNull(resource.message)
    }

    @Test
    fun resourceError_containsMessageAndOptionalData() {
        val message = "Test Error"
        val data = "Error Data"
        val resource = Resource.Error(message, data)
        val resourceWithoutData = Resource.Error<String>(message)

        assertTrue(resource is Resource.Error)
        assertEquals(message, resource.message)
        assertEquals(data, resource.data)

        assertNull(resourceWithoutData.data)
        assertEquals(message, resourceWithoutData.message)
    }

    @Test
    fun resourceLoading_containsOptionalData() {
        val data = "Loading Data"
        val resource = Resource.Loading(data)
        val resourceWithoutData = Resource.Loading<String>()

        assertTrue(resource is Resource.Loading)
        assertEquals(data, resource.data)
        assertNull(resource.message)

        assertNull(resourceWithoutData.data)
        assertNull(resourceWithoutData.message)
    }
}
