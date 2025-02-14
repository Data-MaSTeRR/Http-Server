import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import static com.sun.net.httpserver.HttpServer.create;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class WebServer {

    // "/tasks"와 "/status" 엔드포인트를 상수로 정의 (현재 "/status"는 사용되지 않고 "/tasks"만 사용됨)
    private static final String TASK_ENDPOINT = "/tasks";
    private static final String STATUS_ENDPOINT = "/status";

    private final int port;       // 서버가 실행될 포트
    private HttpServer server;    // HTTP 서버 객체

    // 메인 메서드: 서버 포트를 인자로 받아 WebServer 객체를 생성하고 서버를 시작함
    public static void main(String[] args) {
        int serverPort = 8080;   // 기본 포트 8080
        if (args.length == 1) {
            // 명령행 인자로 전달된 포트 번호 사용
            serverPort = Integer.parseInt(args[0]);
        }

        // WebServer 객체 생성 및 서버 시작
        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();

        System.out.println("Server is listening on port " + serverPort);
    }

    // 생성자: 포트 번호를 초기화
    public WebServer(int port) {
        this.port = port;
    }

    // 서버를 초기화하고 엔드포인트 핸들러를 등록한 후 실행함
    public void startServer() {
        try {
            // 지정된 포트에 바인딩하여 HttpServer 객체 생성 (백로그는 0)
            this.server = create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // "/tasks" 엔드포인트에 대한 컨텍스트 생성
        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);

        // 각각의 컨텍스트에 핸들러 등록
        // (현재 두 컨텍스트 모두 TASK_ENDPOINT로 동일하게 등록되어 있음)
        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);

        // 최대 8개의 스레드를 사용하는 쓰레드 풀을 실행기로 지정
        server.setExecutor(Executors.newFixedThreadPool(8));
        // 서버 시작
        server.start();
    }

    // "/tasks" 엔드포인트의 POST 요청을 처리하는 핸들러 메서드
    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        // POST 방식이 아닌 요청은 처리하지 않고 연결 종료
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();
        // 헤더에 "X-Test"가 존재하고 값이 "true"인 경우, 더미 응답을 반환
        if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "123\n";
            sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }

        // 디버그 모드 여부 확인 ("X-Debug" 헤더가 "true"이면)
        boolean isDebugMode = false;
        if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
            isDebugMode = true;
        }

        // 연산 시작 시간 기록 (나노초 단위)
        long startTime = System.nanoTime();

        // 요청 본문을 바이트 배열로 읽어들임
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        // 요청 바이트 배열에 대한 연산 결과 계산
        byte[] responseBytes = caculateResponse(requestBytes);

        // 연산 완료 시간 기록
        long finishTime = System.nanoTime();

        // 디버그 모드인 경우, 연산 소요 시간을 "X-Debug-Info" 헤더에 추가
        if (isDebugMode) {
            String debugMessage = String.format("Operation took %d ns\n", finishTime - startTime);
            exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        // 계산 결과를 응답으로 전송
        sendResponse(responseBytes, exchange);
    }

    // 요청 바이트 배열에 담긴 문자열을 쉼표(,) 단위로 분리한 후, 각 숫자들의 곱셈 결과를 계산
    private byte[] caculateResponse(byte[] requestBytes) {
        // 요청 본문을 문자열로 변환
        String bodyString = new String(requestBytes);
        // 쉼표(,) 기준으로 숫자 문자열 배열 생성
        String[] stringNumbers = bodyString.split(",");

        BigInteger result = BigInteger.ONE;

        // 각 숫자 문자열을 BigInteger로 변환 후 누적 곱셈 수행
        for (String number : stringNumbers) {
            BigInteger bigInteger = new BigInteger(number);
            result = result.multiply(bigInteger);
        }

        // 계산 결과를 포함한 메시지를 생성하여 바이트 배열로 반환
        return String.format("Result of the multiplication is %s\n", result).getBytes();
    }

    // "/tasks" 엔드포인트의 GET 요청을 처리하는 핸들러 메서드 (서버 상태 체크)
    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        // GET 방식이 아닌 요청은 처리하지 않고 연결 종료
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.close();
            return;
        }

        // 서버 상태 메시지 생성
        String responseMessage = "Server is alive";
        // 응답 전송
        sendResponse(responseMessage.getBytes(), exchange);
    }

    // 응답 바이트 배열을 클라이언트로 전송하는 공통 메서드
    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        // HTTP 상태 코드 200과 응답 바이트 길이로 헤더 전송
        exchange.sendResponseHeaders(200, responseBytes.length);
        // 응답 본문 출력 스트림 가져오기
        OutputStream outputStream = exchange.getResponseBody();
        // 응답 바이트 배열을 스트림에 작성
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }
}
