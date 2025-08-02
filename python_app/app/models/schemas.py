# python_app/app/schemas.py
from pydantic import BaseModel
from pydantic import Field

class GoogleTokenRequest(BaseModel):
    id_token: str

class NaverTokenRequest(BaseModel):
    access_token: str = Field(..., alias="access_token")
class TokenResponse(BaseModel):
    custom_token: str
