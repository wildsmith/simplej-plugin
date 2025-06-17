// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

internal class BgtExtensionsTests {
    private lateinit var application: Application

    @BeforeEach
    fun setUp() {
        application = mockk()
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns application
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `executeBackgroundTask delegates to Application's executeOnPooledThread`() {
        val task = mockk<() -> Unit>(relaxed = true)
        every { application.executeOnPooledThread(any()) } answers {
            firstArg<Runnable>().run()
            fakeFuture()
        }

        executeBackgroundTask(task)

        verify {
            ApplicationManager.getApplication()
            application.executeOnPooledThread(any())
            task.invoke()
        }
    }

    @Test
    fun `executeBackgroundTask executes the provided task`() {
        val future = CompletableFuture<Boolean>()
        every { application.executeOnPooledThread(any()) } answers {
            firstArg<Runnable>().run()
            fakeFuture()
        }

        executeBackgroundTask { future.complete(true) }

        val result = future.get(1, TimeUnit.SECONDS)
        assert(result) { "Task was not executed" }

        verify {
            ApplicationManager.getApplication()
            application.executeOnPooledThread(any())
        }
    }

    @Test
    fun `executeBackgroundTask handles exceptions gracefully`() {
        val exception = RuntimeException("Test exception")
        val future = CompletableFuture<Throwable>()

        every { application.executeOnPooledThread(any()) } answers {
            try {
                firstArg<Runnable>().run()
            } catch (e: Throwable) {
                future.complete(e)
            }
            fakeFuture()
        }

        executeBackgroundTask { throw exception }

        val caught = future.get(1, TimeUnit.SECONDS)
        assertEquals(caught, exception)

        verify {
            ApplicationManager.getApplication()
            application.executeOnPooledThread(any())
        }
    }

    @Test
    fun `executeBackgroundTask executes tasks independently`() {
        val futures = List(3) { CompletableFuture<Int>() }
        every { application.executeOnPooledThread(any()) } answers {
            firstArg<Runnable>().run()
            fakeFuture()
        }

        futures.forEachIndexed { index, future ->
            executeBackgroundTask {
                future.complete(index)
            }
        }

        val results = futures.map { it.get(1, TimeUnit.SECONDS) }
        assert(results == listOf(0, 1, 2)) { "Tasks were not executed independently" }

        verify(exactly = 3) {
            ApplicationManager.getApplication()
            application.executeOnPooledThread(any())
        }
    }

    private fun fakeFuture() = object : Future<Unit> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = true
        override fun get(): Unit = Unit
        override fun get(timeout: Long, unit: TimeUnit): Unit = Unit
    }
}
