# services/firebase_client.py

from firebase_admin import firestore, auth as fb_auth
from .firebase_admin_init import default_app

# Firestore 클라이언트: default_app 으로 초기화된 앱 인스턴스 사용
db = firestore.client(app=default_app)

# Auth 클라이언트는 firebase_admin.auth 모듈을 그대로 사용
# 예) fb_auth.create_custom_token(uid)