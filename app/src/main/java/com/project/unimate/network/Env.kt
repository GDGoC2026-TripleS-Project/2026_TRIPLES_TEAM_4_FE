package com.project.unimate.network

object Env {

    /**
     * [LOCAL TEST - Android Emulator]
     * 로컬에서 백엔드(Spring Boot)를 띄워서 테스트할 때 사용
     * - Android Emulator 기준 PC의 localhost → 10.0.2.2
     * - 예: http://localhost:8080
     */
    const val BASE_URL = "http://unimate-alb-274308250.ap-northeast-2.elb.amazonaws.com"

    /*
    ======================================================
    [PROD / SWAGGER TEST]
    프론트 팀원이 Swagger(UI) 기준으로 API 테스트할 때 사용하는 주소
    - Swagger 주소:
      https://seok-hwan1.duckdns.org/swagger-ui/index.html
    - 실제 API Base URL:
      https://seok-hwan1.duckdns.org

    👉 운영 서버 + Swagger 기준 테스트 시 아래 주소로 교체해서 사용
    ======================================================

    const val BASE_URL = "http://unimate-alb-274308250.ap-northeast-2.elb.amazonaws.com"
    */
}
