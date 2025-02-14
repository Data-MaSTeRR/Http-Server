import com.sun.net.httpserver.*;
import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class WebServer {

    // 서버에서 사용할 엔드포인트를 상수로 정의
    private static final String TASK_ENDPOINT = "/tasks";
    private static final String STATUS_ENDPOINT = "/status";

    private final int port; // 서버가 실행될 포트 번호
    private HttpServer server; // HTTP 서버 객체

    // 메인 메서드: 서버 실행 시작점
    public static void main(String[] args) {
        int serverPort = 8080; // 기본 포트 번호 8080
        if (args.length == 1) {
            // 명령행 인자로 전달된 포트 번호 사용
            serverPort = Integer.parseInt(args[0]);
        }

        // WebServer 객체를 생성하여 서버 시작
        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();

        System.out.println("Server is listening on port " + serverPort);
    }

    // 생성자: 서버 포트 번호를 초기화
    public WebServer(int port) {
        this.port = port;
    }

    // 서버를 시작하고 엔드포인트 핸들러 등록
    public void startServer() {
        try {
            // 지정된 포트에서 서버를 생성
            this.server = create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 엔드포인트 핸들러 설정
        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);

        // 각 엔드포인트에 대한 핸들러 지정
        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);

        // 멀티스레드 환경을 위한 스레드 풀 생성 (최대 8개의 스레드 지원)
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start(); // 서버 시작
    }

    // "/tasks" 엔드포인트의 POST 요청을 처리하는 메서드
    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        try {
            // POST 요청이 아닌 경우 연결 종료
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.close();
                return;
            }

            Headers headers = exchange.getRequestHeaders();

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
            byte[] responseBytes = caculateResponse(requestBytes);

            long finishTime = System.nanoTime(); // 연산 종료 시간 기록

            // 디버그 모드일 경우 실행 시간을 응답 헤더에 포함
            if (isDebugMode) {
                String debugMessage = String.format("Operation took %d ns", finishTime - startTime);
                exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
            }

            sendResponse(responseBytes, exchange);
        } catch (Exception e) {
            e.printStackTrace(); // 서버 콘솔에 에러 출력
            sendResponse("Internal Server Error".getBytes(), exchange);
        }
    }

    // 요청된 숫자들을 곱한 결과를 반환하는 메서드
    private byte[] caculateResponse(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        System.out.println("Received body: [" + bodyString + "]");

        // 입력값을 쉼표로 분리하여 숫자로 변환
        String[] stringNumbers = bodyString.split(",");
        BigInteger result = BigInteger.ONE;

        for (String number : stringNumbers) {
            String trimmed = number.trim();
            System.out.println("Parsing number: [" + trimmed + "]");
            if(trimmed.isEmpty()){
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
        sendResponse("Server is alive".getBytes(), exchange);
    }

    // 클라이언트에게 응답을 보내는 메서드
    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
            outputStream.flush();
        }
    }
}
