package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
// Javadoc: https://www.jacoco.org/jacoco/trunk/doc/api/index.html
import org.junit.jupiter.api.Test;

public class TestTranslateColors
{
    @DisplayName("Test TranslateColors can accept null")
    @Test
    public void TestTranslateNull()
    {
        // Make sure that given nulls, translate colors will return a null.
        assertEquals(null, TranslationUtil.TranslateColors(null, null));
    }

    @DisplayName("Test TranslateColors can accept a formatted string")
    @Test
    public void TestTranslateString()
    {
        // Make sure that given a string, it translates the colors
        assertEquals("bnyeh", TranslationUtil.TranslateColors("&", "bnyeh"));
    }

    @DisplayName("Test TranslateColors can accept color formatting")
    @Test
    public void TestTranslateColoredString()
    {
        // Make sure that given a string with colors, it returns a colored string
        assertEquals("\u00A74bnyeh", TranslationUtil.TranslateColors("&", "&4bnyeh"));
    }

    @DisplayName("Test TranslateColors can accept different format characters")
    @Test
    public void TestTranslateCharacter()
    {
        // Make sure that the color char can be changed to something else
        assertEquals("\u00A74bnyeh", TranslationUtil.TranslateColors("$", "$4bnyeh"));
    }
}