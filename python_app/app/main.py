# python_app/app/main.py

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import auth as fb_auth
import google.auth.transport.requests
import google.oauth2.id_token

from .firebase_admin_init import default_app  # 초기화만 호출됨
from .schemas import TokenRequest, TokenResponse

app = FastAPI()

# CORS 설정: Android 에뮬레이터/디바이스에서 호출 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],        # 실제 배포 시 도메인만 명시하세요
    allow_methods=["POST"],     # 엔드포인트가 POST 하나뿐이므로
    allow_headers=["*"],
)

@app.post("/google-login", response_model=TokenResponse)
def google_login(body: TokenRequest):
    """
    1) Android 클라이언트가 보낸 Google ID 토큰 검증
    2) Firebase Admin SDK로 해당 uid에 대한 커스텀 토큰 생성
    3) 커스텀 토큰을 JSON으로 반환
    """
    try:
        # --- (1) Google ID 토큰 검증 ---
        # verify_oauth2_token는 issuer, 만료시간, signature를 모두 체크
        request = google.auth.transport.requests.Request()
        id_info = google.oauth2.id_token.verify_oauth2_token(
            body.id_token, request
        )
        uid = id_info.get("sub")  # Google 계정마다 고유한 사용자 ID

        # --- (2) Firebase용 커스텀 토큰 생성 ---
        # create_custom_token: uid 기반으로 Firebase 인증에 사용할 토큰 생성
        custom_token_bytes = fb_auth.create_custom_token(uid)
        custom_token = custom_token_bytes.decode("utf-8")

        # --- (3) 토큰 반환 ---
        return TokenResponse(custom_token=custom_token)

    except ValueError as e:
        # ID 토큰이 유효하지 않을 때
        raise HTTPException(status_code=400, detail=f"Invalid Google ID token: {e}")
    except Exception as e:
        # Firebase Admin SDK 에러 등
        raise HTTPException(status_code=500, detail=f"Server error: {e}")