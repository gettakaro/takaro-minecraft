plugins {
    `java-library`
}

dependencies {
    implementation(libs.java.websocket)
    implementation(libs.gson)
    compileOnly(libs.log4j.api)
}
