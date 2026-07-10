// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.mappls.com/repository/mappls/") }
    }
    dependencies {
        // Mappls Services Plugin: reads the <appId>.a.conf / <appId>.a.olf
        // configuration files (new auth mechanism, replaces API keys).
        classpath("com.mappls.services:mappls-services:1.0.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.secrets) apply false
}
