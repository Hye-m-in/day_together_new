package com.example.day_together.ui.onboarding

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.pager.*
import com.example.day_together.R // R 클래스 경로 확인
import com.example.day_together.ui.auth.LoginScreen // LoginScreen 임포트
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.PagerIndicatorActive // Color.kt에 정의된 색상
import com.example.day_together.ui.theme.PagerIndicatorInactive // Color.kt에 정의된 색상

data class OnboardingPageItem(
    val imageRes: Int,
    val title: String,
    val description: String
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(navController: NavController) {

    val pagerState = rememberPagerState()
    val onboardingPages = listOf(
        OnboardingPageItem(
            imageRes = R.drawable.ic_cloud_sad,
            title = "가족과의 대화,\n점점 줄어들고 있진 않나요?",
            description = "바쁜 일상 속 놓치기 쉬운 소중한 대화의 순간들\n돌아보는 시간을 가져보세요."
        ),
        OnboardingPageItem(
            imageRes = R.drawable.ic_cloud_happy,
            title = "소소한 대화가 만드는\n따뜻한 가족의 유대감",
            description = "하루함께가 건네는 한 줄 질문으로\n서로의 하루를 채워 보세요"
        )
    )


    Day_togetherTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                count = 3,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> OnboardingPageContent(item = onboardingPages[0])
                    1 -> OnboardingPageContent(item = onboardingPages[1])
                    2 -> LoginScreen(navController = navController, fromOnboarding = true)
                }
            }

            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 32.dp),
                activeColor = PagerIndicatorActive,
                inactiveColor = PagerIndicatorInactive,
                indicatorWidth = 10.dp,
                indicatorHeight = 10.dp,
                spacing = 8.dp
            )
        }
    }
}

@Composable
fun OnboardingPageContent(item: OnboardingPageItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp)
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        Image(
            painter = painterResource(id = item.imageRes),
            contentDescription = item.title,
            modifier = Modifier
                .size(width = 800.dp, height = 440.dp)
                .padding(bottom = 0.dp)
        )

        Spacer(modifier = Modifier.height(0.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Divider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 20.dp)
        )
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(0.25f))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingScreenPreview() {
    Day_togetherTheme {
        OnboardingScreen(navController = rememberNavController())
    }
}

