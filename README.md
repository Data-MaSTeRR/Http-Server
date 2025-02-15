# HTTP Server

이 프로젝트는 Java 내장 HTTP 서버(`com.sun.net.httpserver.HttpServer`)를 사용하여 간단한 HTTP 서버를 구현한 예제입니다.

## 주요 기능

- **POST /tasks**  
  클라이언트가 쉼표로 구분된 숫자 문자열 (예: `50,100`)을 전송하면, 해당 숫자들의 곱셈 결과를 반환합니다.
  - **디버그 모드:** 요청 헤더에 `X-Debug: true`를 포함하면, 응답 헤더 `X-Debug-Info`에 연산에 소요된 시간을 나노초 단위로 전달합니다.
  - **테스트 모드:** 요청 헤더에 `X-Test: true`를 포함하면, 실제 계산 대신 더미 응답 `123`을 반환합니다.

- **GET /status**  
  서버 상태를 확인하는 엔드포인트로, "Server is alive" 메시지를 반환합니다.

> **주의:** 서버는 `/tasks`와 `/status` 엔드포인트만 지원합니다. `/task` 또는 다른 URL로 요청 시 404 Not Found 에러가 발생합니다.

## 엔드포인트 요약

| HTTP Method | URL     | 설명                                    |
|-------------|---------|-----------------------------------------|
| POST        | /tasks  | 쉼표로 구분된 숫자들의 곱셈 계산           |
| GET         | /status | 서버 상태 확인 ("Server is alive" 메시지) |

## 사용 예

### 서버 실행

서버는 기본적으로 포트 `8080`에서 실행되며, 실행 시 포트 번호를 인자로 전달할 수 있습니다.

```bash
# 기본 포트 8080으로 실행
java -jar target/httpserver-1.0-SNAPSHOT.jar

# 포트 8081로 실행
java -jar target/httpserver-1.0-SNAPSHOT.jar 8081
```

### POST /tasks 요청 예시

1. **일반 곱셈 작업 요청:**

   ```bash
   curl --request POST --data '50,100' localhost:8080/tasks
   ```

   **응답:**

   ```
   Result of the multiplication is 5000
   ```

2. **디버그 모드 요청:**

   ```bash
   curl --request POST --header "X-Debug: true" --data '50,100' localhost:8080/tasks
   ```

   **응답 헤더 예시:**

   ```
   X-Debug-Info: Operation took 4611875 ns
   ```

   **본문 응답:**

   ```
   Result of the multiplication is 5000
   ```

3. **테스트 모드 요청 (더미 응답):**

   ```bash
   curl --request POST --header "X-Test: true" --data '50,100' localhost:8080/tasks
   ```

   **응답:**

   ```
   123
   ```

### GET /status 요청 예시

```bash
curl --request GET localhost:8080/status
```

**응답:**

```
Server is alive
```

## 빌드 및 실행

### Maven 빌드

프로젝트는 Maven을 사용하여 빌드합니다.

```bash
mvn clean package
```

빌드 시 아래와 같은 경고가 발생할 수 있습니다:

```
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-jar-plugin is missing.
```

이는 `maven-jar-plugin`의 버전이 명시되지 않아 발생하는 경고로, 실행에는 큰 영향이 없으나 필요에 따라 `pom.xml`에 플러그인 버전을 추가할 수 있습니다.

### 실행

빌드 후 생성된 JAR 파일을 실행합니다.

```bash
java -jar target/httpserver-1.0-SNAPSHOT.jar [포트번호]
```

포트 번호를 지정하지 않으면 기본적으로 `8080` 포트에서 서버가 실행됩니다.

## 주의 사항

- **엔드포인트 URL:**  
  서버는 `/tasks`와 `/status` 엔드포인트만 지원합니다. 다른 URL로 요청하면 404 Not Found 에러가 발생합니다.

- **HTTP 헤더 주의:**  
  디버그 모드에서 응답 헤더 `X-Debug-Info`에는 개행 문자(`\n`)가 포함되지 않도록 해야 합니다. 개행 문자가 포함되면 500 Internal Server Error가 발생할 수 있습니다.

- **요청 데이터 형식:**  
  POST 요청 본문에는 쉼표로 구분된 숫자 문자열 (예: `50,100`)을 보내야 하며, 각 숫자 앞뒤의 불필요한 공백은 내부에서 `trim()`으로 제거됩니다.

- **에러 처리:**  
  POST 요청 처리 시 발생하는 예외는 서버 콘솔에 스택 트레이스로 출력되며, 클라이언트에는 500 Internal Server Error로 응답됩니다. 문제 발생 시 서버 콘솔 로그를 확인하세요.

## 문제 해결

- **500 Internal Server Error 발생 시:**  
  서버 콘솔의 스택 트레이스(예외 메시지)를 확인하여 발생한 예외(예: `NumberFormatException` 등)를 파악합니다.  
  주로 숫자 파싱 문제 또는 HTTP 헤더에 부적절한 값(개행 문자 포함)으로 인해 발생할 수 있습니다.

- **404 Not Found 에러:**  
  요청한 URL이 `/tasks` 또는 `/status`와 일치하는지 확인하세요.

## 분산시스템을 적용한 http-client와 같이 사용하세요!
