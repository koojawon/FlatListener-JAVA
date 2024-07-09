# Flat Listener(JAVA)

AI 트래킹/PDF-MXL 변환 OCR 프로세스 관리용 서버 서비스 입니다.

RabbitMQ를 통해 메인 서버와 통신하며, PDF 변환 요청 수신 시 Spring WebFlux를 이용한
요청으로 파일을 다운로드 받은 후, OCR 프로세스를 실행하여 변환한 후, 서버에 다시 업로드 합니다.

이후, 트래킹 서비스 요청이 올 경우, 메시지 큐에 들어온 Sdp 정보가 포함된 offer를 이용해 클라이언트와 연결한 후,
MediaStreamTrack의 바이너리 데이터에 접근해 AI 트래킹 프로세스의 파이프를 통해 데이터를 전달, 이후 출력값을 WebRTC DataChannel을 통해
다시 클라이언트에게 전달합니다.