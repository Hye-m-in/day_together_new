package com.example.day_together.data.repository

import com.example.day_together.data.model.User


/**
 * 1단계: AuthRepository 생성
 * 2단계: FakeRepository)과 실제 Firebase DB와 통신하는 AuthRepositoryImpl.kt -> AuthRepository에 맞게 작성
 * 3단계: ViewModel 코드 변경
 *
 * 예시)
 *ui/auth/AuthViewModel.kt
 * [수정 전]
 * private val repository = FakeRepository()
 *[수정 후]
 * private val repository: AuthRepository = FakeRepository()
 */

/**
 * 인증 관련 데이터 처리를 위한 설계도(Interface)입니다.
 * FakeRepository와 나중에 만들 실제 Firebase Repository가 모두 이 설계도를 따르게 됩니다.
 *
 * @see FakeRepository // 이 설계도를 따르는 가짜 구현체
 * @see AuthRepositoryImpl // 나중에 만들 실제 Firebase 구현체
 */
interface AuthRepository {

    /**
     * 현재 로그인된 사용자의 전체 정보를 가져옵니다.
     * @return 로그인된 사용자 정보(User) 객체. 로그인 상태가 아니면 null을 반환합니다.
     */
    suspend fun getCurrentUser(): User?

    /**
     * 이메일과 비밀번호로 로그인을 시도합니다.
     * @param email 사용자가 입력한 이메일
     * @param password 사용자가 입력한 비밀번호
     * @return 로그인 성공 또는 실패 결과를 담은 AuthResult 객체
     */
    suspend fun login(email: String, password: String): AuthResult

    /**
     * 신규 사용자를 회원가입시킵니다.
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 회원가입 성공 또는 실패 결과를 담은 AuthResult 객체
     */
    suspend fun signUp(name: String, email: String, password: String): AuthResult

    /**
     * 현재 사용자를 로그아웃시킵니다.
     */
    fun logout()

    /**
     * 비밀번호 재설정을 요청합니다.
     * @param email 재설정 이메일을 받을 주소
     * @return 요청 성공 또는 실패 결과를 담은 AuthResult 객체
     */
    suspend fun resetPassword(email: String): AuthResult

    /**
     * 이름과 이메일로 아이디(이메일)를 찾습니다.
     * @param name 사용자 이름
     * @param email 사용자 이메일 (확인용)
     * @return 찾기 성공 또는 실패 결과를 담은 AuthResult 객체
     */
    suspend fun findId(name: String, email: String): AuthResult

    /**
     * 변경된 사용자 정보를 DB에 업데이트합니다.
     * @param updatedUser 수정된 정보가 담긴 User 객체
     */
    suspend fun updateUser(updatedUser: User)

    /**
     * 사용자의 비밀번호를 변경합니다.
     * @param email 비밀번호를 변경할 계정의 이메일
     * @param newPassword 새로운 비밀번호
     */
    suspend fun changePassword(email: String, newPassword: String)
}
