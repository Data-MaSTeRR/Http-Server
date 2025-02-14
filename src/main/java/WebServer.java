import com.sun.net.httpserver.*; // 기본적인 HTTP 서버를 구현하는 데 사용
import java.io.*; // 파일 및 네트워크 입출력을 위한 패키지
import java.math.BigInteger; // 큰 정수 연산을 위한 패키지
import java.net.InetSocketAddress; // 특정 주소 및 포트에서 서버를 실행하도록 설정하는 클래스
import java.util.Arrays; // 배열 조작을 위한 유틸리티
import java.util.concurrent.Executors; // 멀티스레드 처리를 위한 Executor 서비스
import static com.sun.net.httpserver.HttpServer.create; // HttpServer의 create 메서드를 직접 호출하기 위해 정적 임포트

public class WebServer {

    // 서버에서 사용할 엔드포인트를 상수로 정의
    private static final String TASK_ENDPOINT = "/tasks"; // 클라이언트가 연산을 요청하는 엔드포인트
    private static final String STATUS_ENDPOINT = "/status"; // 서버 상태를 확인하는 엔드포인트

    private final int port; // 서버가 실행될 포트 번호를 저장하는 변수
    private HttpServer server; // HTTP 서버 객체를 저장하는 변수

    // 메인 메서드: 서버 실행의 시작점
    public static void main(String[] args) {
        int serverPort = 8080; // 기본 포트 번호 8080으로 설정
        if (args.length == 1) { // 만약 명령행 인자가 하나만 주어졌다면
            serverPort = Integer.parseInt(args[0]); // 포트 번호를 명령행 인자로부터 설정
        }

        // WebServer 객체를 생성하여 서버 실행
        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();

        System.out.println("Server is listening on port " + serverPort); // 서버 시작 메시지 출력
    }

    // 생성자: 서버 포트 번호를 초기화하는 역할
    public WebServer(int port) {
        this.port = port; // 인자로 받은 포트 번호를 클래스 변수에 저장
    }

    // 서버를 시작하고 엔드포인트 핸들러를 등록하는 메서드
    public void startServer() {
        try {
            // 지정된 포트에서 HTTP 서버를 생성
            this.server = create(new InetSocketAddress(port), 0);
        } catch (IOException e) { // 서버 생성 중 오류 발생 시
            throw new RuntimeException(e); // 런타임 예외로 변환하여 프로그램 종료
        }

        // 엔드포인트 핸들러 설정
        HttpContext statusContext = server.createContext(STATUS_ENDPOINT); // 서버 상태 확인 엔드포인트 설정
        HttpContext taskContext = server.createContext(TASK_ENDPOINT); // 연산 수행 엔드포인트 설정

        // 각 엔드포인트에 대한 핸들러 지정
        statusContext.setHandler(this::handleStatusCheckRequest); // 상태 체크 핸들러 연결
        taskContext.setHandler(this::handleTaskRequest); // 연산 처리 핸들러 연결

        // 멀티스레드 환경을 위한 스레드 풀 생성 (최대 8개의 스레드 사용)
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start(); // 서버 실행 시작
    }

    // "/tasks" 엔드포인트의 POST 요청을 처리하는 메서드
    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        try {
            // 요청이 POST인지 확인 (POST가 아니면 응답 없이 종료)
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.close();
                return;
            }

            Headers headers = exchange.getRequestHeaders(); // 요청 헤더를 가져옴

            // X-Test 헤더가 "true"이면 더미 응답 반환
            if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
                sendResponse("123\n".getBytes(), exchange);
                return;
            }

            // X-Debug 헤더가 "true"이면 디버그 모드 활성화
            boolean isDebugMode = headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true");

            long startTime = System.nanoTime(); // 연산 시작 시간 기록

            // 요청 본문을 읽고 처리
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            byte[] responseBytes = calculateResponse(requestBytes); // 아래 메서드 참고

            long finishTime = System.nanoTime(); // 연산 종료 시간 기록

            // 디버그 모드일 경우 실행 시간을 응답 헤더에 포함
            if (isDebugMode) {
                String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
                exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
            }

            sendResponse(responseBytes, exchange); // 클라이언트에게 응답 전송
        } catch (Exception e) { // 예외 발생 시
            e.printStackTrace(); // 서버 콘솔에 오류 메시지 출력
            sendResponse("Internal Server Error".getBytes(), exchange); // 500 에러 메시지 전송
        }
    }

    // 요청된 숫자들을 곱한 결과를 반환하는 메서드
    private byte[] calculateResponse(byte[] requestBytes) {
        String bodyString = new String(requestBytes); // 요청 본문을 문자열로 변환
        System.out.println("Received body: [" + bodyString + "]"); // 본문 내용 출력

        // 입력값을 쉼표로 분리하여 숫자로 변환
        String[] stringNumbers = bodyString.split(",");
        BigInteger result = BigInteger.ONE; // 결과값 초기화 (곱셈이므로 1)

        for (String number : stringNumbers) { // 입력값을 하나씩 처리
            String trimmed = number.trim(); // 공백 제거
            System.out.println("Parsing number: [" + trimmed + "]"); // 숫자 변환 과정 출력
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("빈 숫자 값이 포함되어 있습니다.");
            }
            BigInteger bigInteger = new BigInteger(trimmed);
            result = result.multiply(bigInteger);
        }

        String response = String.format("Result of the multiplication is %s\n", result);
        System.out.println("Calculation result: " + response);
        return response.getBytes();
    }

    // "/status" 엔드포인트의 GET 요청을 처리하는 메서드
    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        // GET 요청이 아닐 경우 요청 무시
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.close();
            return;
        }
        sendResponse("Server is alive".getBytes(), exchange); // 상태 메시지 응답
    }

    // 클라이언트에게 응답을 보내는 메서드
    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length); // 200 OK 상태 코드 전송
        try (OutputStream outputStream = exchange.getResponseBody()) { // 응답 본문 전송
            outputStream.write(responseBytes);
            outputStream.flush();
        }
    }
}
