package com.gathin.tvhome;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DockInputsTest {
    private static final String HW15 = "com.tcl.tvinput/.TvPassThroughService/HW15";

    @Test public void thePortNameMatchesWhateverTheSpacingAndCase() {
        assertTrue(DockInputs.matches("input:HDMI 1", HW15, "HDMI 1"));
        assertTrue(DockInputs.matches("input:hdmi1", HW15, "HDMI 1"));
        assertTrue(DockInputs.matches("input:HDMI-1", HW15, "HDMI 1"));
    }

    @Test public void anotherPortDoesNotMatch() {
        assertFalse(DockInputs.matches("input:HDMI 1", HW15, "HDMI 11"));
        assertFalse(DockInputs.matches("input:HDMI 2", HW15, "HDMI 1"));
    }

    @Test public void theInputIdMatchesExactly() {
        assertTrue(DockInputs.matches("input:" + HW15, HW15, "HDMI 1"));
        assertFalse(DockInputs.matches("input:" + HW15.toLowerCase(), HW15, "HDMI 2"));
    }

    @Test public void onlyEntriesWithSomethingAfterThePrefixAreInputs() {
        assertTrue(DockInputs.isInput("input:HDMI 1"));
        assertFalse(DockInputs.isInput("input:"));
        assertFalse(DockInputs.isInput("com.netflix.ninja"));
        assertTrue(DockInputs.any(Arrays.asList("com.spotify.tv.android", "input:HDMI 1")));
        assertFalse(DockInputs.any(Arrays.asList("com.spotify.tv.android")));
    }

    @Test public void punctuationAloneMatchesNothing() {
        assertFalse(DockInputs.matches("input: - ", HW15, "--"));
    }
}
