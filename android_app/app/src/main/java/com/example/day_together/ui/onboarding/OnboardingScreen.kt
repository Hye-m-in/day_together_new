package com.example.day_together.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.day_together.R
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.ui.theme.*
import com.google.accompanist.pager.*

/**
 * 온보딩 화면이 끝까지 스와이프 된 경우, 더 이상 온보딩 화면 노출x : 데이터 및 캐시 삭제 필요
 * 온보딩 각 페이지에 표시될 내용을 담는 데이터 클래스
 */
data class OnboardingPageItem(
    val imageRes: Int,
    val title: String,
    val description: String
)

/**
 * 앱 첫 실행 시 보여주는 온보딩 화면의 전체 UI
 *
 *
 * @param navController 화면 이동 컨트롤러
 * @param pagerState 좌우로 넘기는 페이지의 상태를 관리
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    pagerState: PagerState
) {
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
            // 좌우로 스와이프 가능한 페이지 뷰
            HorizontalPager(
                count = 3, // 온보딩 2개 + 시작하기 1개
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> OnboardingPageContent(item = onboardingPages[0])
                    1 -> OnboardingPageContent(item = onboardingPages[1])
                    // 세 번째 페이지에는 로그인 화면으로 이동하는 버튼이 있는 페이지
                    2 -> StartPageContent(
                        onStartClicked = {
                            navController.navigate(AppDestinations.LOGIN_ROUTE) {
                                // 온보딩 화면은 뒤로가기로 돌아올 수 없도록 스택에서 제거
                                popUpTo(AppDestinations.ONBOARDING_ROUTE) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // 현재 페이지 위치를 알려주는 인디케이터 (점 3개)
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

/**
 * 온보딩의 각 페이지 내용을 그리는 컴포저블
 */
@Composable
fun OnboardingPageContent(item: OnboardingPageItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = item.imageRes),
            contentDescription = item.title,
            modifier = Modifier.aspectRatio(1f).padding(bottom = 24.dp)
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 온보딩 마지막 페이지의 내용을 그리는 컴포저블
 */
@Composable
fun StartPageContent(onStartClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_cloud_happy), // 이미지
            contentDescription = "시작하기",
            modifier = Modifier.aspectRatio(1f).padding(bottom = 24.dp)
        )
        Text(
            text = "이제, 가족과 함께\n하루를 채워볼까요?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            onClick = onStartClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonActiveBackground
            )
        ) {
            Text("시작하기", style = MaterialTheme.typography.labelLarge)
        }
    }
}
