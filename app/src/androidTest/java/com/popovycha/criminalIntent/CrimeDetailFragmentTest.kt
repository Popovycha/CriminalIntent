package com.popovycha.criminalIntent

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CrimeDetailFragmentTest {

    private lateinit var scenario: FragmentScenario<CrimeDetailFragment>

    @Before
    fun setup() {
        scenario = launchFragmentInContainer() // new
    }

    @After
    fun tearDown() {
        scenario.close()
    }


    @Test fun checkbox_updates_crime() {
        onView(withId(R.id.crime_solved))
            .perform(click())

        onView(withId(R.id.crime_solved))
            .check(matches(isChecked()))
    }
}