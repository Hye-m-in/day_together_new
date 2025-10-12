plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.day_together"
    compileSdk = 35

    buildFeatures{
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.example.day_together"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // 네이버 로그인 관련 설정
        // Manifest의 <data android:scheme="${naverClientId}"/> 에 주입될 값은
        // "항상 소문자"여야 경고/에러가 나지 않음 (스킴은 소문자만 허용)
        // gradle.properties의 NAVER_CLIENT_ID가 대문자여도 여기서 lowercase()로 강제 변환
        // 기본값도 소문자로 둠
        manifestPlaceholders["naverClientId"] =
            providers.gradleProperty("NAVER_CLIENT_ID")
                .orNull
                ?.lowercase()              // 소문자 강제
                ?: "your_naver_client_id"  // 기본값도 소문자

        // 아래 BuildConfig 값들은 앱 내부 로직에서 쓰는 상수이므로
        // 원문 그대로(대소문자 유지) 쓰는 것이 보통 더 안전함
        // 즉, 스킴만 소문자 강제, 나머지 상수는 원래 케이스 유지
        val naverClientId = providers.gradleProperty("NAVER_CLIENT_ID").get()
        val naverClientSecret = providers.gradleProperty("NAVER_CLIENT_SECRET").get()
        val naverClientName = providers.gradleProperty("NAVER_CLIENT_NAME").get()

        buildConfigField("String", "NAVER_CLIENT_ID", "\"$naverClientId\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"$naverClientSecret\"")
        buildConfigField("String", "NAVER_CLIENT_NAME", "\"$naverClientName\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // 이미지 로딩
    implementation(libs.coil.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Compose UI 계열
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Navigation
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // Accompanist
    val accompanistVersion = "0.34.0"
    implementation("com.google.accompanist:accompanist-pager:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-pager-indicators:$accompanistVersion")

    // JDK desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // AndroidX Lifecycle / Activity
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // AppCompat / Material / ConstraintLayout
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // 네트워킹(Volley)
    implementation(libs.volley)
    // Retrofit (네트워킹 라이브러리)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson Converter (JSON <-> Kotlin 데이터 클래스 자동 변환)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Media3(필요시)
    implementation(libs.androidx.media3.common.ktx)

    // Crashlytics 빌드툴
    implementation(libs.firebase.crashlytics.buildtools)

    // Test/Debug
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Coroutines
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")


    //retrofit2관련 의존성
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")

    //Jake Wharton 코루틴 어댑터
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")

    // 코루틴
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // 코루틴 (Android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // OkHttp + 로깅 인터셉터
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    //Naver 의존성
    implementation("com.navercorp.nid:oauth:5.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Firebase Modules
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // 구글 로그인
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // 네이버 로그인 SDK (화면 띄우기용)
    implementation("com.navercorp.nid:oauth:5.10.0")
    implementation("androidx.browser:browser:1.8.0") // Custom Tabs 사용
}
